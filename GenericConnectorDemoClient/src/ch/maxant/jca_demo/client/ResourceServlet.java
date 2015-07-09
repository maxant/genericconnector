/*
   Copyright 2015 Ant Kutschera

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
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
