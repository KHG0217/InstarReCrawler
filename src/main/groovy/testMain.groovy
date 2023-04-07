import java.util.concurrent.LinkedBlockingQueue

class testMain {
    public static void main(String[] args) {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.add("item1");
        queue.add("item2");
        queue.add("item3");

        // remove "item2" from the queue
        boolean removed = queue.remove("item2");
        if (removed) {
            System.out.println("item2 removed successfully");
        } else {
            System.out.println("item2 not found in the queue");
        }

        System.out.println("Queue after removal: " + queue);
    }

}
