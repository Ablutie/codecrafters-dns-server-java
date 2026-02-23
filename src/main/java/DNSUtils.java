import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DNSUtils {

    public static DNSMessage echoMessage(DNSMessage message) {

        List<Answer> answers = message.getQuestions().stream()
                .map(Question::question)
                .map(Answer::defaultAnswer)
                .toList();

        return new DNSMessage.Builder()
                .transactionId(message.getTransactionId())
                .queryIndicator(true)
                .opCode(message.getOpCode())
                .recursionDesired(message.isRecursionDesired())
                .questionCount(message.getQuestionCount())
                .answerRecordCount(message.getQuestionCount())
                .questions(message.getQuestions())
                .answers(answers)
                .build();
    }

    public static DNSMessage parsePacket(byte[] arr) {

        // transaction id
        int transactionId = arr[0] & 0xFF;
        transactionId = (transactionId << 8) + (arr[1] & 0xFF);

        // header flags and codes
        byte headers1 = arr[2];
        boolean queryIndicator = ((headers1 & 0b10000000) >> 7) == 1;
        byte opCode = (byte) ((headers1 & 0b01111000) >> 3);
        boolean recursionDesired = ((headers1 & 0b00000001) == 1);

        // number of questions
        int numQuestions = arr[4] & 0xFF;
        numQuestions = (numQuestions << 8) + (arr[5] & 0xFF);

        // number of answers
        int numAnswers = arr[6] & 0xFF;
        numAnswers = (numAnswers << 8) + (arr[7] & 0xFF);

        // questions
        List<Question> questions = parseQuestions(arr, numQuestions);

        // answers
        int questionOffset = getQuestionOffset(questions);
        List<Answer> answers = parseAnswers(arr, numAnswers, questionOffset);

        return new DNSMessage.Builder()
                .transactionId((short) transactionId)
                .queryIndicator(queryIndicator)
                .opCode(opCode)
                .recursionDesired(recursionDesired)
                .questionCount(numQuestions)
                .answerRecordCount(numAnswers)
                .questions(questions)
                .answers(answers)
                .build();
    }

    private static List<Answer> parseAnswers(byte[] arr, int numAnswers, int questionOffset) {

        List<Answer> answers = new ArrayList<>();

        int start = 13 + questionOffset;
        for (int i = 0; i < numAnswers; i++) {
            Answer answer = parseAnswer(arr, start, false);
            answers.add(answer);
            start += (answer.realLength()) + 1;
        }

        return answers;
    }

    private static List<Question> parseQuestions(byte[] arr, int numQuestions) {

        List<Question> questions = new ArrayList<>();
        int start = 12;
        for (int i = 0; i < numQuestions; i++) {
            Question question = parseQuestion(arr, start);
            questions.add(question);
            start += (question.realLength()) + 1;
        }

        return questions;
    }

//    public static Answer parseAnswer(byte[] arr, int start) {
//
//        StringBuilder domain = new StringBuilder();
//
//        List<String> questions = parseQuestions(arr, 1);
//        int start = 12 + questions.getFirst().length() + 6;
//
//        int wordLength = arr[start];
//        int currentIndex = start + 1;
//
//        while (true) {
//            byte[] wordBytes = new byte[wordLength];
//            System.arraycopy(arr, currentIndex, wordBytes, 0, wordLength);
//
//            String word = new String(wordBytes, StandardCharsets.US_ASCII);
//            domain.append(word);
//            currentIndex += wordLength;
//
//            int currentByte = arr[currentIndex];
//            if (currentByte == 0) {
//                break;
//            } else {
//                wordLength = arr[currentIndex];
//                currentIndex++;
//                domain.append(".");
//            }
//        }
//
//        // skip TYPE + CLASS - hardcoded to type A + class IN
//        currentIndex += 5;
//
//        // TTL has the next 4 bytes
//        byte[] ttl = new byte[4];
//        System.arraycopy(arr, currentIndex, ttl, 0, 4);
//
//        // move 6 bytes forward - 4 for the TTL and 2 for hardcoded RDLENGTH
//        currentIndex += 6;
//        byte[] ip = new byte[4];
//        System.arraycopy(arr, currentIndex, ip, 0, 4);
//
//        return new Answer(domain.toString(), ttl, ip);
//    }

    private static Answer parseAnswer(byte[] arr, int start, boolean recursive) {

        StringBuilder domain = new StringBuilder();

        int currentIndex = start;
        int answerLength = 0;

        if (hasPointer(arr[currentIndex])) {
            Answer pointerDomain = parseAnswer(arr, getPointer(arr[currentIndex], arr[currentIndex + 1]), true);
            domain.append(pointerDomain.resource());
            // add pointer octets to question length
            answerLength += 2;
        } else {
            byte wordLength = arr[start];
            currentIndex++;

            while (true) {
                byte[] wordBytes = new byte[wordLength];
                System.arraycopy(arr, currentIndex, wordBytes, 0, wordLength);

                String word = new String(wordBytes, StandardCharsets.US_ASCII);
                domain.append(word);
                answerLength += word.length();
                currentIndex += wordLength;

                byte currentByte = arr[currentIndex];
                if (currentByte == 0) {
                    // add null length octet, 2 type bytes, 2 class bytes
                    // 4 ttl bytes, 2 rdlength bytes and 4 rdata bytes (assumes it's always ip)
                    answerLength += 15;
                    break;
                } else if (hasPointer(currentByte)) {
                    domain.append(".");
                    Answer pointerDomain = parseAnswer(arr, getPointer(currentByte, arr[currentIndex + 1]), true);
                    domain.append(pointerDomain.resource());
                    // add pointer octets to question length
                    answerLength += 2;
                    break;
                } else {
                    domain.append(".");
                    answerLength++;
                    wordLength = currentByte;
                    currentIndex++;
                }
            }
        }

        if (recursive) {
            return Answer.defaultAnswer(domain.toString());
        }

        // skip TYPE + CLASS - hardcoded to type A + class IN
        currentIndex += 6;

        // TTL has the next 4 bytes
        byte[] ttl = new byte[4];
        System.arraycopy(arr, currentIndex, ttl, 0, 4);

        // move 6 bytes forward - 4 for the TTL and 2 for hardcoded RDLENGTH
        currentIndex += 6;
        byte[] ip = new byte[4];
        System.arraycopy(arr, currentIndex, ip, 0, 4);

        return new Answer(domain.toString(), answerLength, ttl, ip);
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
                Question pointerDomain = parseQuestion(arr, getPointer(currentByte, arr[currentIndex + 1]));
                domain.append(pointerDomain.question());
                // add pointer octet to question length
                questionLength += 2;
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

    private static int getPointer(byte firstByte, byte secondByte) {

        int pointer = firstByte  & 0b00111111;
        return (pointer << 8) + (secondByte & 0xFF);

//        int numQuestions = arr[4] & 0xFF;
//        numQuestions = (numQuestions << 8) + (arr[5] & 0xFF);
    }

    private static int getQuestionOffset(List<Question> questions) {

        return questions.stream()
                .map(Question::realLength)
                .mapToInt(Integer::intValue)
                .sum();
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

        // RA + Z + AD + CD

        String sb = "0000"

                // RCODE
                + (message.getOpCode() == 0 ? "0000" : "0100");

        return Integer.valueOf(sb, 2).byteValue();
    }

    private static byte[] getQuestionSectionAsBytes(DNSMessage message) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        for (Question question : message.getQuestions()) {
            String[] words = question.question().split("\\.");

            for (String word : words) {
                bos.write((byte) word.length());
                bos.write(word.getBytes(StandardCharsets.US_ASCII));
            }
            bos.write((byte) 0);

            // hardcode to RR type A, class IN
            bos.write(new byte[]{0, 1, 0, 1});
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
            bos.write(new byte[]{0, 1, 0, 1});

            bos.write(answer.ttl());

            // hardcode length to 4
            bos.write(new byte[]{0, 4});

            bos.write(answer.ip());
        }

        return bos.toByteArray();
    }

}
