package chat.client;

public class J3hwABC {
    private final Object lock = new Object();
    private volatile char currentChar = 'A';

    public static void main(String[] args) {
        J3hwABC waitNotifyObj = new J3hwABC();
        Thread thread1 = new Thread(() -> {
            waitNotifyObj.printA();
        });
        Thread thread2 = new Thread(() -> {
            waitNotifyObj.printB();
        });
        Thread thread3 = new Thread(() -> {
            waitNotifyObj.printC();
        });
        thread1.start();
        thread2.start();
        thread3.start();
    }

    public void printA() {
        synchronized (lock) {
            try {
                for (int i = 0; i < 5; i++) {
                    while (currentChar != 'A') {
                        lock.wait();
                    }
                    System.out.print("A");
                    currentChar = 'B';
                    lock.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void printB() {
        synchronized (lock) {
            try {
                for (int i = 0; i < 5; i++) {
                    while (currentChar != 'B') {
                        lock.wait();
                    }
                    System.out.print("B");
                    currentChar = 'C';
                    lock.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void printC() {
        synchronized (lock) {
            try {
                for (int i = 0; i < 5; i++) {
                    while (currentChar != 'C') {
                        lock.wait();
                    }
                    System.out.print("C");
                    currentChar = 'A';
                    lock.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
