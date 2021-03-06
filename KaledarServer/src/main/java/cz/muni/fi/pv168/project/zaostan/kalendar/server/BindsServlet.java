package cz.muni.fi.pv168.project.zaostan.kalendar.server;

import cz.muni.fi.pv168.project.zaostan.kalendar.entities.Bind;
import cz.muni.fi.pv168.project.zaostan.kalendar.entities.User;
import cz.muni.fi.pv168.project.zaostan.kalendar.exceptions.binding.BindingException;
import cz.muni.fi.pv168.project.zaostan.kalendar.exceptions.user.UserException;
import cz.muni.fi.pv168.project.zaostan.kalendar.managers.BindManager;
import cz.muni.fi.pv168.project.zaostan.kalendar.managers.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author Peter Stanko
 * @date 20.4.2015
 */
@WebServlet(BindsServlet.URL_MAPPING + "/*")
public class BindsServlet extends HttpServlet {
    public static final String URL_MAPPING = "/binds";
    public static final String BINDS_JSP = "/binds.jsp";
    public static final String BINDS_MANAGER = "BindsManager";
    final static Logger logger = LoggerFactory.getLogger(BindsServlet.class);
    private static final String UPDATE_FLAG = "update_flag";


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        logger.info("Incomming get request");
        try {
            logger.info("Showing users...");
            showBinds(request, response, false);
        } catch (BindingException ex) {
            logger.error("Problem with showing users.", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private void showBinds(HttpServletRequest request, HttpServletResponse response, boolean update) throws BindingException, ServletException, IOException {
        BindManager bindManager = getBindsManager(request);
        if (bindManager == null) {
            logger.error("Bind manager is NULL ----------------------");
        }
        List<Bind> allBinds = bindManager.getAllBindings();
        if (allBinds == null) {
            logger.error("All binds are null -------------------------");
        }

        request.setAttribute("Users", allBinds);
        if(update)
            request.setAttribute(UPDATE_FLAG, true);
        request.getRequestDispatcher(BINDS_JSP).forward(request, response);
    }

    private BindManager getBindsManager(HttpServletRequest request) {
        return (BindManager) getServletContext().getAttribute(BINDS_MANAGER);
    }


}
