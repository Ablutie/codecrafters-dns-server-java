import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DNSMessage {

    private final short transactionId;
    private final boolean queryIndicator;
    private final byte opCode;
    private final boolean authoritativeAnswer;
    private final boolean truncation;
    private final boolean recursionDesired;
    private final boolean recursionAvailable;
    private final byte reserved;
    private final byte responseCode;
    private final short questionCount;
    private final short answerRecordCount;
    private final short authorityRecordCount;
    private final short additionalRecordCount;

    private final String question;

    DNSMessage(Builder builder) {

        this.transactionId = builder.transactionId;
        this.queryIndicator = builder.queryIndicator;
        this.opCode = builder.opCode;
        this.authoritativeAnswer = builder.authoritativeAnswer;
        this.truncation = builder.truncation;
        this.recursionDesired = builder.recursionDesired;
        this.recursionAvailable = builder.recursionAvailable;
        this.reserved = builder.reserved;
        this.responseCode = builder.responseCode;
        this.questionCount = builder.questionCount;
        this.answerRecordCount = builder.answerRecordCount;
        this.authorityRecordCount = builder.authorityRecordCount;
        this.additionalRecordCount = builder.additionalRecordCount;
        this.question = builder.question;
    }

    public byte[] toByteArray() {
        byte[] response = new byte[512];

        // transaction ID
        response[0] = (byte) (transactionId >> 8);
        response[1] = (byte) (transactionId & 0xFF);

        // flags
        response[2] = (byte) 0b10000000;

        // question count
        response[5] = (byte) 1;

        // questions
        try {
            byte[] question = getQuestionAsBytes();
            System.arraycopy(question, 0, response, 12, question.length);
        } catch (IOException e) {
            System.out.printf("Could not write question to response: %s%n", e.getMessage());
        }

        return response;
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
        bos.write(new byte[] {1, 1});

        return bos.toByteArray();
    }

    public static class Builder {
        private short transactionId;
        private boolean queryIndicator;
        private byte opCode;
        private boolean authoritativeAnswer;
        private boolean truncation;
        private boolean recursionDesired;
        private boolean recursionAvailable;
        private byte reserved;
        private byte responseCode;
        private short questionCount;
        private short answerRecordCount;
        private short authorityRecordCount;
        private short additionalRecordCount;
        private String question;
        
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

        public Builder authoritativeAnswer(boolean authoritativeAnswer) {

            this.authoritativeAnswer = authoritativeAnswer;
            return this;
        }

        public Builder truncation(boolean truncation) {

            this.truncation = truncation;
            return this;
        }

        public Builder recursionDesired(boolean recursionDesired) {

            this.recursionDesired = recursionDesired;
            return this;
        }

        public Builder recursionAvailable(boolean recursionAvailable) {

            this.recursionAvailable = recursionAvailable;
            return this;
        }

        public Builder reserved(byte reserved) {

            this.reserved = reserved;
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

        public DNSMessage build() {
            return new DNSMessage(this);
        }

    }
}
