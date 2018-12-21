package sec.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import sec.project.CyberSecurityBaseProjectApplication;
import sec.project.domain.Signup;
import sec.project.repository.SignupRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Controller
public class SignupController {

    @Autowired
    private SignupRepository signupRepository;

    @Autowired
    private HttpSession session;

    @Autowired
    private HttpServletRequest request;

    private int maxTrys = 3;

    private Connection connection = CyberSecurityBaseProjectApplication.connection;

    @RequestMapping("*")
    public String defaultMapping() {
        return "redirect:/form";
    }

    @RequestMapping(value = "/form", method = RequestMethod.GET)
    public String loadForm() {
        return "form";
    }

    @RequestMapping(value = "/form", method = RequestMethod.POST)
    public String submitForm(@RequestParam String name, @RequestParam String address) {
        signupRepository.save(new Signup(name, address));
        /* check for database entry
        System.out.println(signupRepository.findAll().size());
        System.out.println(signupRepository.findAll().get(0).getName());
        System.out.println(signupRepository.findAll().get(0).getAddress());
        System.out.println(signupRepository.findAll().get(signupRepository.findAll().size()-1).getName());
        System.out.println(signupRepository.findAll().get(signupRepository.findAll().size()-1).getAddress());
        */
        return "done";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String loadLogin() {
        return "login";
    }

    @RequestMapping(value = "/adminLogin", method = RequestMethod.POST)
    public String login(@RequestParam String name, @RequestParam String password) {
        try {
            // check ip and count login attempts

            String ip = "000.000.000.000";
            String xfHeader = request.getHeader("X-Forwarded-For");
            //System.out.println(xfHeader);
            if (xfHeader == null){
                ip =  request.getRemoteAddr();
            } else {ip = xfHeader.split(",")[0];}
            //System.out.println(ip);

            // check if ip is already in db
            String ipcheckquery = "SELECT ip FROM ipBlock WHERE ip= ?";
            PreparedStatement ipcheckpstmt = connection.prepareStatement( ipcheckquery );
            ipcheckpstmt.setString( 1, ip);
            ResultSet ipinfo = ipcheckpstmt.executeQuery();

            if(ipinfo.next()) {
                // get attempts from DB
                String attemptsquery = "SELECT attempts FROM ipBlock WHERE ip= ?";
                PreparedStatement attemptspstmt = connection.prepareStatement( attemptsquery );
                attemptspstmt.setString( 1, ip);
                ResultSet attempts = attemptspstmt.executeQuery();

                // check if attempts is higher then max value
                attempts.next(); // don't forget it!!! otherwise it crashes
                if(attempts.getInt("attempts") >= maxTrys) {
                    System.out.println("more then " + maxTrys + " attempts by: " + ip);
                    return "login";
                }
                // increase value of attempts and write back to database
                int update = attempts.getInt("attempts") + 1;
                String attemptsupdatequery = "UPDATE ipBlock SET attempts = ? WHERE ip = ?";
                PreparedStatement attemptsupdatespstmt = connection.prepareStatement( attemptsupdatequery );
                attemptsupdatespstmt.setString( 1, String.valueOf(update));
                attemptsupdatespstmt.setString( 2, ip);
                attemptsupdatespstmt.executeUpdate();
            } else {
                //insert ip, that tryed to login, in database, if it is not in it yet
                String updatequery = "INSERT INTO ipblock (ip, attempts) VALUES (?, 1)";
                PreparedStatement updatepstmt = connection.prepareStatement( updatequery );
                updatepstmt.setString( 1, ip);
                updatepstmt.executeUpdate();
                }


            // broken system for SQL injection
            /*
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT password FROM accounts WHERE name='" + name + "' and password='" + password + "'");
            System.out.println(resultSet.next()); // resultSet.next() has to executed once otherwise it can't find database (gets used below)
            */

            // solution for SQL injection
            String query = "SELECT password FROM accounts WHERE name = ? and password = ?";
            PreparedStatement pstmt = connection.prepareStatement( query );
            pstmt.setString( 1, name);
            pstmt.setString( 2, password);
            ResultSet resultSet = pstmt.executeQuery();

            if(resultSet.next()) {
                // because password and username are correct set attempts back to 0
                String attemptsresetquery = "UPDATE ipBlock SET attempts = ? WHERE ip = ?";
                PreparedStatement attemptsresetspstmt = connection.prepareStatement( attemptsresetquery );
                attemptsresetspstmt.setString( 1, String.valueOf(0));
                attemptsresetspstmt.setString( 2, ip);
                attemptsresetspstmt.executeUpdate();



                /*
                added my own session ident which is set if password and username fit
                and gets set to null if user leaves site
                /adminDashboard is only loading if ident is set correct
                */
                session.setAttribute("ident", "qwertzuiopasdfghjklyxcvbnm");
                return "adminDashboard";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        session.setAttribute("ident", "null");
        return "redirect:/login";
    }

    // prevent URL access to adminDashboard
    @RequestMapping(value = "/adminDashboard", method = RequestMethod.GET)
    public String listSubscribers(Model model) {
        //if(session.getAttribute("ident") == "qwertzuiopasdfghjklyxcvbnm") {
            model.addAttribute("adminDashboard", signupRepository.findAll());
            return "adminDashboard";
        //}
        //return("login");
    }

    @RequestMapping(value = "/deleteSignUps", method = RequestMethod.GET)
    public String deleteSignUps() {
        if(session.getAttribute("ident") == "qwertzuiopasdfghjklyxcvbnm") {
            signupRepository.deleteAll();
            return("redirect:/adminDashboard");
        }
        return("redirect:/login");
    }
}
