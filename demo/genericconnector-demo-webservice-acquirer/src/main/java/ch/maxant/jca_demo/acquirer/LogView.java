package ch.maxant.jca_demo.acquirer;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/LogView")
public class LogView extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("<html><body><h2>Acquirer</h2><table>")
		.append("<tr><td>executions</td><td>").append(String.valueOf(AcquirerWebService.CALLS_EXECUTE.get())).append("</td></tr>")
		.append("<tr><td>commits</td><td>").append(String.valueOf(AcquirerWebService.CALLS_COMMIT.get())).append("</td></tr>")
		.append("<tr><td>rollbacks</td><td>").append(String.valueOf(AcquirerWebService.CALLS_ROLLBACK.get())).append("</td></tr>")
		.append("<tr><td>recovery</td><td>").append(String.valueOf(AcquirerWebService.CALLS_RECOVER.get())).append("</td></tr>")
		.append("</table></html>");
	}

}
