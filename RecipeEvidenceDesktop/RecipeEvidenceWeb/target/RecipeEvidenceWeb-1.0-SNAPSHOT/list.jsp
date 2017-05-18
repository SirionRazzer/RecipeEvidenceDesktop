<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body>

<table border="1">
    <thead>
    <tr>
        <th>název</th>
        <th>procedura</th>
        <th>ingredience</th>
    </tr>
    </thead>
    <c:forEach items="${recipes}" var="recipe">
        <tr>
            <td><c:out value="${recipe.name}"/></td>
            <td><c:out value="${recipe.procedure}"/></td>
            <td><c:out value="${recipe.ingredients}"/></td>
            <td><form method="post" action="${pageContext.request.contextPath}/recipes/delete?id=${recipe.id}"
                      style="margin-bottom: 0;"><input type="submit" value="Smazat"></form></td>
        </tr>
    </c:forEach>
</table>

<h2>Zadejte recept</h2>
<c:if test="${not empty chyba}">
    <div style="border: solid 1px red; background-color: yellow; padding: 10px">
        <c:out value="${chyba}"/>
    </div>
</c:if>
<form action="${pageContext.request.contextPath}/recipes/add" method="post">
    <table>
        <tr>
            <th>název receptu:</th>
            <td><input type="text" name="name" value="<c:out value='${param.name}'/>"/></td>
        </tr>
        <tr>
            <th>procedura:</th>
            <td><input type="text" name="procedure" value="<c:out value='${param.procedure}'/>"/></td>
        </tr>
        <tr>
            <th>ingredience:</th>
            <td><input type="text" name="ingredience" value="<c:out value='${param.ingredience}'/>"/></td>
        </tr>
    </table>
    <input type="Submit" value="Zadat" />
</form>

</body>
</html>
