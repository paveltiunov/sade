<%@ page import="javax.naming.InitialContext" %>
<%@ page import="org.sade.Sade" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Sade sade = (Sade) InitialContext.doLookup("ejblocal:org.sade.Sade");
%>
<html>
<head><title>Simple jsp page</title></head>
<body><%=sade.sayHello("fooBar")%></body>
</html>