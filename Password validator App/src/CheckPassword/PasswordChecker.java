/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CheckPassword;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.*;
import java.time.LocalDate;

public class PasswordChecker {

/*
    
sql script for database
create database passwordchecker;
use  passwordchecker;
create table pwd(
id int,
username varchar(50),
password varchar(50),
active boolean ,
date date
);
    
    */

    static boolean checker(String password) {
        //       REGULAR EXPRESSION FOR DIFFERENT PATTERNS
        String isNumberPresent = ".*[0-9].*";
        String isUpperCasePresent = ".*[A-Z].*";
        String isLowerCasePresent = ".*[a-z].*";
        String isSpecialCharacterPresent = "[a-zA-Z0-9 ]*";

        //       digit pattern 
        Pattern digitPattern = Pattern.compile(isNumberPresent);
        // match for uppercase
        Pattern upperCasePattern = Pattern.compile(isUpperCasePresent);

        //        match for lowercase
        Pattern lowerCasePattern = Pattern.compile(isLowerCasePresent);
        //match for special characters
        Pattern specialPattern = Pattern.compile("[a-zA-Z0-9 ]*");
        final String defaultPassword = "Test123@test";
        if (password.equals(defaultPassword)) {
            System.out.println("This password is not valid try a different password");
            return false;
        }
        if (password.trim() == null) {
            System.out.println("whitespaces not allowed in the password");
        }
        if (password.length() < 8) {
            System.out.println("password must be at least 8 character long");
            return false;
        }

//       MATCH FOR AT LEAST ONE DIGIT
        Matcher digitMatcher = digitPattern.matcher(password);
        if (!digitMatcher.matches()) {
            System.out.println("Password must contain at least one digit");
            return false;
        }
//Match for at least one upper case character
        Matcher upperCaseMatcher = upperCasePattern.matcher(password);
        if (!upperCaseMatcher.matches()) {
            System.out.println("Password must contain at least one uppercase character");
            return false;
        }
//        Match for at least one lower case Character
        Matcher lowerCaseMatcher = lowerCasePattern.matcher(password);
        if (!lowerCaseMatcher.matches()) {
            System.out.println("Password must contain at least one lowercase character");
            return false;
        }

//        Matching for specail characters
        Matcher specialCharMatcher = specialPattern.matcher(password);
        if (specialCharMatcher.matches()) {
            System.out.println("At least one special character must be present");
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
//database configuration
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/passwordchecker";
        String uName = "root";
        String pass = "Best123@best";
        Connection con = DriverManager.getConnection(url, uName, pass);
//end data base configuration
        Scanner scan = new Scanner(System.in);
        System.out.println("1.Login\n2.Register");
        int n = scan.nextInt();

//        logic for user login 
        if (n == 1) {
            Scanner in = new Scanner(System.in);
            System.out.println("Enter Registered Username");
            String rusername = in.nextLine();
            PreparedStatement stmnt = con.prepareStatement("select username from pwd where username=?");
            stmnt.setString(1, rusername);
            ResultSet users = stmnt.executeQuery();
            if (!users.next()) {
                System.out.println("NO such user exists please register first");
                return;
            }

            PreparedStatement stm = con.prepareStatement("select active from pwd where username=?");
            stm.setString(1, rusername);
            ResultSet res = stm.executeQuery();
            res.next();
            if (res.getBoolean(1) == false) {
                System.out.println("This is locked account can not login");
                return;
            }

//            get user account expiration date
            PreparedStatement stm1 = con.prepareStatement("select date from pwd where username=?");
            stm1.setString(1, rusername);
            ResultSet res2 = stm1.executeQuery();
            res2.next();
            LocalDate local = LocalDate.now();
            Date todayDate = Date.valueOf(local);

            if (todayDate.compareTo(res2.getDate(1)) > 0 || todayDate.compareTo(res2.getDate(1)) == 0) {
                System.out.println("You Must change your password");
                while (true) {
                    System.out.println("Enter new  password");
                    Scanner pa = new Scanner(System.in);
                    String password = pa.next();
                    if (!checker(password)) {
                        return;
                    }
                    String query2 = "select password from (select *from pwd order by id desc limit 5)q";
                    Statement stment = con.createStatement();
                    ResultSet pas = stment.executeQuery(query2);
                    boolean flag = false;
                    while (pas.next()) {
                        if (pas.getString(1).equals(password)) {
                            flag = true;
                            break;
                        }
                    }
//           password is one of the last passwords
                    if (flag) {
                        System.out.println("This password is not allowed try a different password");
                    } else {
                        PreparedStatement stmn = con.prepareStatement("update pwd set password=?,date=? where username=?");
                        stmn.setString(1, password);
                        stmn.setString(3, rusername);
                        LocalDate date = LocalDate.now();
                        date = date.plusDays(14);
                        Date expirationDate = Date.valueOf(date);
                        stmn.setDate(2, expirationDate);
                        stmn.executeUpdate();
                        System.out.println("Password changed Successfully");
                        break;
                    }
                }

                return;
            }

            System.out.println("Enter password");
            String rpassword = in.next();
            PreparedStatement stmnt1 = con.prepareStatement("select password from pwd where username=?");
            stmnt1.setString(1, rusername);
            ResultSet rpass = stmnt1.executeQuery();
            rpass.next();
            int attempts = 4;
            if (!rpass.getString(1).equals(rpassword)) {
                while (attempts > 0 && !rpass.getString(1).equals(rpassword)) {
                    System.out.println("Enter correct password");
                    rpassword = in.next();
                    if (attempts == 3) {
                        System.out.println("Only two attempts left");
                    }
                    attempts--;
                }
            }

            if (attempts == 0 && !rpass.getString(1).equals(rpassword)) {
                PreparedStatement stmn = con.prepareStatement("update pwd set active=false where username=?");
                stmn.setString(1, rusername);
                stmn.executeUpdate();
                System.out.println("Your account is loccked");
                return;
            }
            System.out.println("Logged in successfully");
        }

//        Registering a new user
        if (n == 2) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter Username");
            String username = sc.nextLine();

            Statement statement = con.createStatement();
//                        check if user already exists
            String query3 = "select username from pwd ";
            ResultSet usernameQuery = statement.executeQuery(query3);
            boolean isUserExists = false;
            while (usernameQuery.next()) {
                if (usernameQuery.getString(1).equals(username)) {
                    isUserExists = true;
                    break;
                }
            }
            if (isUserExists) {
                System.out.println("username " + username + " already exists try a different username");
                return;
            }

            System.out.println("Enter your password");
            String password = sc.next();
            boolean isValid = checker(password);
            if (isValid) {
                int id = 0;
                String query = "insert into pwd values(?,?,?,?,?)";
                PreparedStatement st = con.prepareStatement(query);
                PreparedStatement ps = con.prepareStatement("select max(id) from pwd");

                ResultSet rs = ps.executeQuery();

//         get last 5 passwords
                String query2 = "select password from (select *from pwd order by id desc limit 5)q";
                ResultSet pas = statement.executeQuery(query2);

                boolean flag = false;
                while (pas.next()) {
                    if (pas.getString(1).equals(password)) {
                        flag = true;
                        break;
                    }
                }
//           password is one of the last passwords
                if (flag) {
                    System.out.println("This password is not allowed try a different password");
                    return;
                }
// End of the password logic(last 5 passwords
//-------------------------------------------------------------------------------------------------
//Adding new Entry to the database
                while (rs.next()) {
                    id = rs.getInt(1);
                    id++;
                }

                st.setInt(1, id);
                st.setString(2, username);
                st.setString(3, password);
                st.setBoolean(4, true);
                LocalDate date = LocalDate.now();
                date = date.plusDays(14);
                Date today = Date.valueOf(date);
                st.setDate(5, today);
                st.executeUpdate();
                System.out.println("Your account has been created");
                st.close();
                con.close();

            }
        }
    }
}
