import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DNSUtils {

    public static DNSMessage parseRequest(byte[] arr) {

        // transaction id
        int transactionId = arr[0] & 0xFF;
        transactionId = (transactionId << 8) + (arr[1] & 0xFF);

        // header flags and codes
        byte headers1 = arr[2];
        byte opCode = (byte) ((headers1 & 0b01111000) >> 3);
        boolean recursionDesired = ((headers1 & 0b00000001) == 1);

        // number of questions
        int numQuestions = arr[4] & 0xFF;
        numQuestions = (numQuestions << 8) + (arr[5] & 0xFF);

        // questions
        List<String> questions = parseQuestions(arr, numQuestions);

        return new DNSMessage.Builder()
                .transactionId((short) transactionId)
                .queryIndicator(true)
                .opCode(opCode)
                .recursionDesired(recursionDesired)
                .questionCount(numQuestions)
                .answerRecordCount(numQuestions)
                .questions(questions)
                .answers(questions) // mimic question in answer
                .build();
    }

    private static List<String> parseQuestions(byte[] arr, int numQuestions) {
        List<Question> questions = new ArrayList<>();
        int start = 12;
        for (int i = 0; i < numQuestions; i++) {
            Question question = parseQuestion(arr, start);
            questions.add(question);
            start += (question.realLength()) + 1;
        }

        return questions.stream()
                .map(Question::question)
                .toList();
    }

    private static Question parseQuestion(byte[] arr, int start) {

        StringBuilder domain = new StringBuilder();

        int currentIndex = start + 1;
        byte wordLength = arr[start];
        int questionLength = 0;

        while (true) {
            byte[] wordBytes = new byte[wordLength];
            System.arraycopy(arr, currentIndex, wordBytes, 0, wordLength);

            String word = new String(wordBytes, StandardCharsets.US_ASCII);
            domain.append(word);
            questionLength += word.length();
            currentIndex += wordLength;

            byte currentByte = arr[currentIndex];
            if (currentByte == 0) {
                // add null length octet and 4 type / class octets
                questionLength += 5;
                break;
            } else if (hasPointer(currentByte)) {
                domain.append(".");
                Question pointerDomain = parseQuestion(arr, getPointer(currentByte));
                domain.append(pointerDomain.question());
                // add pointer octet to question length
                questionLength++;
                break;
            } else {
                domain.append(".");
                questionLength++;
                wordLength = currentByte;
                currentIndex++;
            }
        }

        return new Question(domain.toString(), questionLength);
    }

    private static boolean hasPointer(byte octet) {
        return ((octet >> 6) & 0b11) == 3;
    }

  private static int getPointer(byte octet) {
        return octet & 0b00111111;
  }

    public static byte[] dnsResponseToByteArray(DNSMessage request) {
        byte[] response = new byte[512];

        // transaction ID
        response[0] = (byte) (request.getTransactionId() >> 8);
        response[1] = (byte) (request.getTransactionId() & 0xFF);

        // flags
        response[2] = getFirstHeaderAsByte(request);
        response[3] = getSecondHeaderAsByte(request);

        // question count
        response[5] = (byte) request.getQuestionCount();

        // answer count
        response[7] = (byte) request.getAnswerRecordCount();

        try {
            // questions
            byte[] questionSection = getQuestionSectionAsBytes(request);
            System.arraycopy(questionSection, 0, response, 12, questionSection.length);

            // answers
            byte[] answerSection = getAnswerSectionAsBytes(request);
            System.arraycopy(answerSection, 0, response, 12 + questionSection.length, answerSection.length);

        } catch (IOException e) {
            System.out.printf("Could not write question to response: %s%n", e.getMessage());
        }

        return response;
    }

    private static byte getFirstHeaderAsByte(DNSMessage message) {
        StringBuilder sb = new StringBuilder();

        // QR - true
        sb.append("1");

        // OPCODE
        String opCodeBinary = String.format("%4s", Integer.toBinaryString(message.getOpCode())).replace(' ', '0');
        sb.append(opCodeBinary);

        // AA + TC
        sb.append("00");

        // RD
        sb.append(message.isRecursionDesired() ? "1" : "0");

        return Integer.valueOf(sb.toString(), 2).byteValue();
    }

    private static byte getSecondHeaderAsByte(DNSMessage message) {
        StringBuilder sb = new StringBuilder();

        // RA + Z + AD + CD
        sb.append("0000");

        // RCODE
        sb.append(message.getOpCode() == 0 ? "0000" : "0100");

        return Integer.valueOf(sb.toString(), 2).byteValue();
    }

    private static byte[] getQuestionSectionAsBytes(DNSMessage message) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        for (String question : message.getQuestions()) {
            String[] words = question.split("\\.");

            for (String word : words) {
                bos.write((byte) word.length());
                bos.write(word.getBytes(StandardCharsets.US_ASCII));
            }
            bos.write((byte) 0);

            // hardcode to RR type A, class IN
            bos.write(new byte[] {0, 1, 0, 1});
        }

        return bos.toByteArray();
    }

    private static byte[] getAnswerSectionAsBytes(DNSMessage message) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        for (String answer : message.getAnswers()) {
            String[] words = answer.split("\\.");

            for (String word : words) {
                bos.write((byte) word.length());
                bos.write(word.getBytes(StandardCharsets.US_ASCII));
            }
            bos.write((byte) 0);

            // hardcode to RR type A, class IN
            bos.write(new byte[] {0, 1, 0, 1});

            // hardcode TTL to 60
            bos.write(new byte[] {0, 0, 0, 60});

            // hardcode length to 4
            bos.write(new byte[] {0, 4});

            // hardcode IP to 124.168.0.1
            bos.write(new byte[] {124, 8, 0, 1});
        }

        return bos.toByteArray();
    }

}
