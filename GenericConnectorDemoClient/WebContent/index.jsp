<%@page import="javax.naming.Context"%>
<%@page import="javax.naming.NameClassPair"%>
<%@page import="javax.naming.NamingEnumeration"%>
<%@page import="javax.naming.InitialContext"%>
<%
String name = request.getParameter("name");
if(name == null){
    name = "java:/";
}
%>
<html>
<h3>JNDI</h3>
<form action="index.jsp" method="get">
Name: <input type="text" name="name" value="<%=name %>"  accesskey="n" />
<input  type="submit" />
</form>
<body>
<%
InitialContext ic = new InitialContext();
try{
	NamingEnumeration<NameClassPair> o = ic.list(name);
	while(o.hasMoreElements()){
	    Object o2 = o.next();
	    if(o2 instanceof Context){
	        Context c = (Context)o2;
	        %><a href="index.jsp?name=<%=c.getNameInNamespace()%>"><%=c.getNameInNamespace()%> (<%=o2%>)</a><br/><%
	    }else{
	        %><%=o2%><br/><%
	    }
	}
}catch(Exception e){
	e.printStackTrace();
    %>Error: <%=e%><br/><%
}
%>
</body>
</html>