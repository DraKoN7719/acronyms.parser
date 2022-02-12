public class Main {
    public static void main(String[] args) {
        Parser parser = args.length == 1 ? new Parser(args[0]) : new Parser();
        parser.start();
    }
}