public record Answer (
        String resource,
        int realLength,
        byte[] ttl,
        byte[] ip
){

    public static Answer defaultAnswer(String resource) {
        return new Answer(resource,
                resource.length(),
                new byte[] {0, 0, 0, 60}, // hardcode TTL to 60
                new byte[] {124, 8, 0, 1}); // hardcode IP to 124.168.0.1
    }
}
