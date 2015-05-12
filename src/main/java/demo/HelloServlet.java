package demo;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@WebServlet(value = "/hello", name = "hello-servlet")
public class HelloServlet extends GenericServlet {
	
	@Autowired Environment env;

	public void service(ServletRequest req, ServletResponse res) throws IOException, ServletException {
		res.getWriter().println("Hello World!");
		res.getWriter().println(env.toString());
	}
}
