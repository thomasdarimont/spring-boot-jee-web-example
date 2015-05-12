package demo;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class HelloListener implements ServletContextListener  {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		System.out.println("Hello from: " + this);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		
	}

}
