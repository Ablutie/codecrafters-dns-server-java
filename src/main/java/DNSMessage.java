import java.util.List;

public class DNSMessage {

    private final short transactionId;
    private final boolean queryIndicator;
    private final byte opCode;
    private final boolean recursionDesired;
    private final int questionCount;
    private final int answerRecordCount;

    private final List<String> questions;
    private final List<Answer> answers;

    DNSMessage(Builder builder) {

        this.transactionId = builder.transactionId;
        this.queryIndicator = builder.queryIndicator;
        this.opCode = builder.opCode;
        this.recursionDesired = builder.recursionDesired;
        this.questionCount = builder.questionCount;
        this.answerRecordCount = builder.answerRecordCount;
        this.questions = builder.questions;
        this.answers = builder.answers;
    }

    public short getTransactionId() {

        return transactionId;
    }

    public boolean isResponse() {

        return queryIndicator;
    }

    public byte getOpCode() {

        return opCode;
    }

    public boolean isRecursionDesired() {

        return recursionDesired;
    }

    public int getQuestionCount() {

        return questionCount;
    }

    public int getAnswerRecordCount() {

        return answerRecordCount;
    }

    public List<String> getQuestions() {

        return questions;
    }

    public List<Answer> getAnswers() {

        return answers;
    }

    public static class Builder {
        private short transactionId;
        private boolean queryIndicator;
        private byte opCode;
        private boolean recursionDesired;
        private int questionCount;
        private int answerRecordCount;
        private List<String> questions;
        private List<Answer> answers;
        
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

        public Builder questionCount(int questionCount) {

            this.questionCount = questionCount;
            return this;
        }

        public Builder answerRecordCount(int answerRecordCount) {

            this.answerRecordCount = answerRecordCount;
            return this;
        }

        public Builder questions(List<String> questions) {
            this.questions = questions;
            return this;
        }

        public Builder answers(List<Answer> answers) {
            this.answers = answers;
            return this;
        }

        public DNSMessage build() {
            return new DNSMessage(this);
        }

    }
}
