/**
 * Created by LXX on 2019/4/1.
 */
public class InterruptDemo {
    static boolean isEnd = false;
    public static void main(String[] args) {
        final Thread sleepThread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                }catch (InterruptedException e){
                    System.out.println("e:"+e.getMessage());
                }
            }
        });
        Thread busyThread = new Thread(){
            @Override
            public void run() {
                while (!isEnd);
            }
        };
        sleepThread.start();
        System.out.println("sleepThread:"+sleepThread.getState());
        busyThread.start();
        System.out.println("busyThread:"+busyThread.getState());
        sleepThread.interrupt();
        System.out.println("sleepThread:"+sleepThread.getState());
        busyThread.interrupt();
        System.out.println("busyThread:"+busyThread.getState());
        while (sleepThread.isInterrupted()){
            System.out.println("sleepThread interrupted");
        };
        System.out.println("sleepThread isInterrupted: " + sleepThread.isInterrupted());
        System.out.println("busyThread isInterrupted: " + busyThread.isInterrupted());
        System.out.println("sleepThread:"+sleepThread.getState());
        System.out.println("busyThread:"+busyThread.getState());
        isEnd = true;
        try {
            busyThread.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        System.out.println("busyThread:"+busyThread.getState());
    }
}
