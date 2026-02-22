public record Answer (
        String resource,
        byte[] ttl,
        byte[] ip
){

    public static Answer defaultAnswer(String resource) {
        return new Answer(resource,
                new byte[] {0, 0, 0, 60}, // hardcode TTL to 60
                new byte[] {124, 8, 0, 1}); // hardcode IP to 124.168.0.1
    }
}
