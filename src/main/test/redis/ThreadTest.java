package redis;


public class ThreadTest {
	public static void main(String[] arg) throws Exception{
		int i = 0 ;
		for(;;){
			System.out.println(++i);
			Thread t = new Thread(){
				public void run(){
					while(true){}
				}
			};
			t.start();
		}
	}
}
