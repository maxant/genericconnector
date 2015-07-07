package ch.maxant.jca_demo.client;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ResourceServlet")
public class ResourceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final Logger log = Logger.getLogger(this.getClass().getName());

    @EJB private SomeServiceThatBindsResourcesIntoTransaction svc;
    
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    String refNum = request.getParameter("refNum");
        if(refNum == null) refNum = "refNum";
        try {
            String s = svc.doSomethingInvolvingSeveralResources(refNum);
            log.log(Level.INFO, "servlet got: " + s);
            response.getWriter().append(s);
        } catch (Exception e) {
            throw new ServletException(e);
        }
	}

}
