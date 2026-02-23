import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DNSUtils {

    public static DNSMessage parsePacket(byte[] arr) {

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

        // number of answers
        int numAnswers = numQuestions;

        // questions
        List<String> questions = parseQuestions(arr, numQuestions);

        // answers
        List<Answer> answers = questions.stream()
                .map(Answer::defaultAnswer)
                .toList();

        return new DNSMessage.Builder()
                .transactionId((short) transactionId)
                .queryIndicator(true)
                .opCode(opCode)
                .recursionDesired(recursionDesired)
                .questionCount(numQuestions)
                .answerRecordCount(numAnswers)
                .questions(questions)
                .answers(answers) // mimic question in answer
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

    public static Answer parseAnswer(byte[] arr) {
        StringBuilder domain = new StringBuilder();

        int start = 12;
        int wordLength = arr[start];
        int currentIndex = start + 1;

        while (true) {
            byte[] wordBytes = new byte[wordLength];
            System.arraycopy(arr, currentIndex, wordBytes, 0, wordLength);

            String word = new String(wordBytes, StandardCharsets.US_ASCII);
            domain.append(word);
            currentIndex += wordLength;

            int currentByte = arr[currentIndex];
            if (currentByte == 0) {
                break;
            } else {
                wordLength = arr[currentIndex];
                currentIndex++;
                domain.append(".");
            }
        }

        // skip TYPE + CLASS - hardcoded to type A + class IN
        currentIndex += 5;

        // TTL has the next 4 bytes
        byte[] ttl = new byte[4];
        System.arraycopy(arr, currentIndex, ttl, 0, 4);

        // move 12 bytes forward - 4 for the TTL and 2 for hardcoded RDLENGTH and 6 more determined empirically
        currentIndex += 12;
        byte[] ip = new byte[4];
        System.arraycopy(arr, currentIndex, ip, 0, 4);

        return new Answer(domain.toString(), ttl, ip);
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

    public static byte[] dnsMessageToByteArray(DNSMessage message) {
        byte[] response = new byte[512];

        // transaction ID
        response[0] = (byte) (message.getTransactionId() >> 8);
        response[1] = (byte) (message.getTransactionId() & 0xFF);

        // flags
        response[2] = getFirstHeaderAsByte(message);
        response[3] = getSecondHeaderAsByte(message);

        // question count
        response[5] = (byte) message.getQuestionCount();

        // answer count
        response[7] = (byte) message.getAnswerRecordCount();

        try {
            // questions
            byte[] questionSection = getQuestionSectionAsBytes(message);
            System.arraycopy(questionSection, 0, response, 12, questionSection.length);

            // answers
            byte[] answerSection = getAnswerSectionAsBytes(message);
            System.arraycopy(answerSection, 0, response, 12 + questionSection.length, answerSection.length);

        } catch (IOException e) {
            System.out.printf("Could not write question to response: %s%n", e.getMessage());
        }

        return response;
    }

    private static byte getFirstHeaderAsByte(DNSMessage message) {
        StringBuilder sb = new StringBuilder();

        // QR - true
        sb.append(message.isResponse() ? "1" : "0");

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

        for (Answer answer : message.getAnswers()) {
            String[] words = answer.resource().split("\\.");

            for (String word : words) {
                bos.write((byte) word.length());
                bos.write(word.getBytes(StandardCharsets.US_ASCII));
            }
            bos.write((byte) 0);

            // hardcode to RR type A, class IN
            bos.write(new byte[] {0, 1, 0, 1});

            bos.write(answer.ttl());

            // hardcode length to 4
            bos.write(new byte[] {0, 4});

            bos.write(answer.ip());
        }

        return bos.toByteArray();
    }

}
