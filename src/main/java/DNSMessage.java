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
    }

    public short getTransactionId() {

        return transactionId;
    }

    public boolean isQueryIndicator() {

        return queryIndicator;
    }

    public byte getOpCode() {

        return opCode;
    }

    public boolean isAuthoritativeAnswer() {

        return authoritativeAnswer;
    }

    public boolean isTruncation() {

        return truncation;
    }

    public boolean isRecursionDesired() {

        return recursionDesired;
    }

    public boolean isRecursionAvailable() {

        return recursionAvailable;
    }

    public byte getReserved() {

        return reserved;
    }

    public byte getResponseCode() {

        return responseCode;
    }

    public short getQuestionCount() {

        return questionCount;
    }

    public short getAnswerRecordCount() {

        return answerRecordCount;
    }

    public short getAuthorityRecordCount() {

        return authorityRecordCount;
    }

    public short getAdditionalRecordCount() {

        return additionalRecordCount;
    }

    public byte[] toByteArray() {
        byte[] arr = new byte[512];

        // transaction ID
        arr[0] = (byte) (transactionId >> 8);
        arr[1] = (byte) (transactionId & 0xFF);

        // flags
        arr[2] = (byte) 0b10000000;

        return arr;
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

        public DNSMessage build() {
            return new DNSMessage(this);
        }

    }
}
