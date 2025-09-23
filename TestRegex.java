public class TestRegex {
    public static void main(String[] args) {
        String region1 = "us-west-2";
        String region2 = "cn-north-1";
        
        System.out.println("us-west-2 matches: " + region1.matches("^[a-z]{2}-[a-z]+-\\d+$"));
        System.out.println("cn-north-1 matches: " + region2.matches("^[a-z]{2}-[a-z]+-\\d+$"));
    }
}