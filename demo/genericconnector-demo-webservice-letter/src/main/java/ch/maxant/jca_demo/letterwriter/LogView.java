package ch.maxant.jca_demo.letterwriter;

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
		response.getWriter().append("<html><body><h2>Letter Writer</h2><table>")
		.append("<tr><td>executions incl. commit</td><td>").append(String.valueOf(LetterWebService.CALLS_EXECUTE.get())).append("</td></tr>")
		.append("<tr><td>rollbacks</td><td>").append(String.valueOf(LetterWebService.CALLS_ROLLBACK.get())).append("</td></tr>")
		.append("</table></html>");
	}

}
