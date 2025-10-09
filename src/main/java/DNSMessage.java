import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DNSMessage {

    private final short transactionId;
    private final boolean queryIndicator;
    private final byte opCode;
    private final boolean recursionDesired;
    private final byte responseCode;
    private final short questionCount;
    private final short answerRecordCount;
    private final short authorityRecordCount;
    private final short additionalRecordCount;

    private final String question;
    private final String answer;

    DNSMessage(Builder builder) {

        this.transactionId = builder.transactionId;
        this.queryIndicator = builder.queryIndicator;
        this.opCode = builder.opCode;
        this.recursionDesired = builder.recursionDesired;
        this.responseCode = builder.responseCode;
        this.questionCount = builder.questionCount;
        this.answerRecordCount = builder.answerRecordCount;
        this.authorityRecordCount = builder.authorityRecordCount;
        this.additionalRecordCount = builder.additionalRecordCount;
        this.question = builder.question;
        this.answer = builder.answer;
    }

    public byte[] toByteArray() {
        byte[] response = new byte[512];

        // transaction ID
        response[0] = (byte) (transactionId >> 8);
        response[1] = (byte) (transactionId & 0xFF);

        // flags
        response[2] = getFirstHeaderByte();
        response[3] = getSecondHeaderByte();

        // question count
        response[5] = (byte) 1;

        // answer count
        response[7] = (byte) 1;

        // questions
        try {
            byte[] question = getQuestionAsBytes();
            System.arraycopy(question, 0, response, 12, question.length);

            byte[] answer = getAnswerAsBytes();
            System.arraycopy(answer, 0, response, 12 + question.length, answer.length);

        } catch (IOException e) {
            System.out.printf("Could not write question to response: %s%n", e.getMessage());
        }

        return response;
    }

    private byte getFirstHeaderByte() {
        StringBuilder sb = new StringBuilder();

        // QR - true
        sb.append("1");

        // OPCODE
        String opCodeBinary = String.format("%4s", Integer.toBinaryString(opCode)).replace(' ', '0');
        sb.append(opCodeBinary);

        // AA + TC
        sb.append("00");

        // RD
        sb.append(recursionDesired ? "1" : "0");

        return Integer.valueOf(sb.toString(), 2).byteValue();
    }

    private byte getSecondHeaderByte() {
        StringBuilder sb = new StringBuilder();

        // RA + Z + AD + CD
        sb.append("0000");

        // RCODE
        sb.append(opCode == 0 ? "0000" : "0100");

        return Integer.valueOf(sb.toString(), 2).byteValue();
    }

    private byte[] getQuestionAsBytes() throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String[] words = question.split("\\.");

        for (String word : words) {
            bos.write((byte) word.length());
            bos.write(word.getBytes(StandardCharsets.US_ASCII));
        }
        bos.write((byte) 0);

        // hardcode to RR type A, class IN
        bos.write(new byte[] {0, 1, 0, 1});

        return bos.toByteArray();
    }

    private byte[] getAnswerAsBytes() throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
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

        return bos.toByteArray();
    }

    public static class Builder {
        private short transactionId;
        private boolean queryIndicator;
        private byte opCode;
        private boolean recursionDesired;
        private byte responseCode;
        private short questionCount;
        private short answerRecordCount;
        private short authorityRecordCount;
        private short additionalRecordCount;
        private String question;
        private String answer;
        
        public Builder transactionId(short transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        
        public Builder queryIndicator(boolean queryIndicator) {
            this.queryIndicator = queryIndicator;
            return this;
        }

        public Builder opCode(byte opCode) {

            this.opCode = opCode;
            return this;
        }

        public Builder recursionDesired(boolean recursionDesired) {

            this.recursionDesired = recursionDesired;
            return this;
        }

        public Builder responseCode(byte responseCode) {

            this.responseCode = responseCode;
            return this;
        }

        public Builder questionCount(short questionCount) {

            this.questionCount = questionCount;
            return this;
        }

        public Builder answerRecordCount(short answerRecordCount) {

            this.answerRecordCount = answerRecordCount;
            return this;
        }

        public Builder authorityRecordCount(short authorityRecordCount) {

            this.authorityRecordCount = authorityRecordCount;
            return this;
        }

        public Builder additionalRecordCount(short additionalRecordCount) {

            this.additionalRecordCount = additionalRecordCount;
            return this;
        }

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder answer(String answer) {
            this.answer = answer;
            return this;
        }

        public DNSMessage build() {
            return new DNSMessage(this);
        }

    }
}
