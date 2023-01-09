package smarticulous;

import smarticulous.db.Exercise;
import smarticulous.db.Submission;
import smarticulous.db.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The Smarticulous class, implementing a grading system.
 */
public class Smarticulous {

    /**
     * The connection to the underlying DB.
     * <p>
     * null if the db has not yet been opened.
     */
    Connection db;

    /**
     * Open the {@link Smarticulous} SQLite database.
     * <p>
     * This should open the database, creating a new one if necessary, and set the {@link #db} field
     * to the new connection.
     * <p>
     * The open method should make sure the database contains the following tables, creating them if necessary:
     *
     * <table>
     *   <caption><em>Table name: <strong>User</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>UserId</td><td>Integer (Primary Key)</td></tr>
     *   <tr><td>Username</td><td>Text</td></tr>
     *   <tr><td>Firstname</td><td>Text</td></tr>
     *   <tr><td>Lastname</td><td>Text</td></tr>
     *   <tr><td>Password</td><td>Text</td></tr>
     * </table>
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>Exercise</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>ExerciseId</td><td>Integer (Primary Key)</td></tr>
     *   <tr><td>Name</td><td>Text</td></tr>
     *   <tr><td>DueDate</td><td>Integer</td></tr>
     * </table>
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>Question</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>ExerciseId</td><td>Integer</td></tr>
     *   <tr><td>QuestionId</td><td>Integer</td></tr>
     *   <tr><td>Name</td><td>Text</td></tr>
     *   <tr><td>Desc</td><td>Text</td></tr>
     *   <tr><td>Points</td><td>Integer</td></tr>
     * </table>
     * In this table the combination of ExerciseId and QuestionId together comprise the primary key.
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>Submission</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>SubmissionId</td><td>Integer (Primary Key)</td></tr>
     *   <tr><td>UserId</td><td>Integer</td></tr>
     *   <tr><td>ExerciseId</td><td>Integer</td></tr>
     *   <tr><td>SubmissionTime</td><td>Integer</td></tr>
     * </table>
     *
     * <p>
     * <table>
     *   <caption><em>Table name: <strong>QuestionGrade</strong></em></caption>
     *   <tr><th>Column</th><th>Type</th></tr>
     *   <tr><td>SubmissionId</td><td>Integer</td></tr>
     *   <tr><td>QuestionId</td><td>Integer</td></tr>
     *   <tr><td>Grade</td><td>Real</td></tr>
     * </table>
     * In this table the combination of SubmissionId and QuestionId together comprise the primary key.
     *
     * @param dburl The JDBC url of the database to open (will be of the form "jdbc:sqlite:...")
     * @return the new connection
     * @throws SQLException
     */

    public Connection openDB(String dburl) throws SQLException {
        // TODO: Implement
        db = DriverManager.getConnection(dburl);
        String table1 = "CREATE TABLE IF NOT EXISTS User" + "(UserId INTEGER PRIMARY KEY, " +
                "Username TEXT UNIQUE, Firstname TEXT, Lastname TEXT, Password TEXT);";
        createAndExecute(table1);

        String table2 = "CREATE TABLE IF NOT EXISTS Exercise" + "(ExerciseId INTEGER " +
                "PRIMARY KEY, Name TEXT, DueDate INTEGER);";
        createAndExecute(table2);

        String table3 = "CREATE TABLE IF NOT EXISTS Question" +
                "(ExerciseId INTEGER, QuestionId INTEGER, Name TEXT, Desc TEXT, " +
                "Points INTEGER, PRIMARY KEY(ExerciseId, QuestionId));";
        createAndExecute(table3);

        String table4 = "CREATE TABLE IF NOT EXISTS Submission" +
                "(SubmissionId INTEGER PRIMARY KEY, UserId INTEGER, " +
                "ExerciseId INTEGER, SubmissionTime INTEGER);";
        createAndExecute(table4);

        String table5 = "CREATE TABLE IF NOT EXISTS QuestionGrade" +
                "(SubmissionId INTEGER, QuestionId INTEGER, Grade REAL, " +
                "PRIMARY KEY(SubmissionId, QuestionId));";
        createAndExecute(table5);
        return db;
    }


    /**
     * Close the DB if it is open.
     *
     * @throws SQLException
     */
    public void closeDB() throws SQLException {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    // =========== User Management =============

    /**
     * Add a user to the database / modify an existing user.
     * <p>
     * Add the user to the database if they don't exist. If a user with user. username does exist,
     * update their password and firstname/lastname in the database.
     *
     * @param user
     * @param password
     * @return the userid.
     * @throws SQLException
     */
    public int addOrUpdateUser(User user, String password) throws SQLException {
        // TODO: Implement
        String sqlQuery = "REPLACE INTO User (Username,Firstname,Lastname,Password) VALUES (?,?,?,?)";
        //PreparedStatement is preferred to prevent injection - safer than string formatting
        PreparedStatement authorizedStatement = this.db.prepareStatement(sqlQuery);
        copyValues(authorizedStatement, sqlQuery, user, password);
        authorizedStatement.execute();
        return authorizedStatement.getGeneratedKeys().getInt(1);

    }

    /**
     * Verify a user's login credentials.
     *
     * @param username
     * @param password
     * @return true if the user exists in the database and the password matches; false otherwise.
     * @throws SQLException
     * <p>
     * Note: this is totally insecure. For real-life password checking, it's important to store only
     * a password hash
     * @see <a href="https://crackstation.net/hashing-security.htm">How to Hash Passwords Properly</a>
     */

  public boolean verifyLogin(String username, String password) throws SQLException {
        // TODO: Implement
        ResultSet queryRes = null;
        PreparedStatement authorizedStatement = null;
        String sqlQuery = "SELECT Password FROM User WHERE Username =?;";

        authorizedStatement = db.prepareStatement(sqlQuery);
        authorizedStatement.setString(1,username);  //get the current username
        queryRes = authorizedStatement.executeQuery();
        if (queryRes.next()) {
            String queryResPassword = queryRes.getString("Password");
            queryRes.close();
            authorizedStatement.close();
            return queryResPassword.equals(password); //return true if match
        }
        // return false if user doesn't exists in DB
        return false;
    }

    // =========== Exercise Management =============

    /**
     * Add an exercise to the database.
     *
     * @param exercise
     * @return the new exercise id, or -1 if an exercise with this id already existed in the database.
     * @throws SQLException
     */
    public int addExercise(Exercise exercise) throws SQLException {
        // TODO: Implement
        String sqlQuery = "INSERT INTO Exercise (ExerciseId, Name, DueDate) VALUES (?,?,?);";
        PreparedStatement authorizedQuery = db.prepareStatement(sqlQuery);
        authorizedQuery.setInt(1, exercise.id);
        authorizedQuery.setString(2, exercise.name);
        authorizedQuery.setDate(3, new Date(exercise.dueDate.getTime()));

        if (authorizedQuery.executeUpdate() == 1) {
            // add/update Exercise's questions to DB
            for (Exercise.Question question : exercise.questions) {
                String query = "REPLACE INTO Question (ExerciseId, QuestionId, Name, Desc, Points) VALUES (?,?,?,?,?);";
                PreparedStatement newStatement = db.prepareStatement(query);
                newStatement.setInt(1, exercise.id);
                newStatement.setString(3, question.name);
                newStatement.setString(4, question.desc);
                newStatement.setInt(5, question.points);
                newStatement.execute();
            }
        }
        else {  // Return -1 if exercise already exists
            return -1;
        }
        return exercise.id;
    }

    /**
     * Return a list of all the exercises in the database.
     * <p>
     * The list should be sorted by exercise id.
     *
     * @return list of all exercises.
     * @throws SQLException
     */
    public List<Exercise> loadExercises() throws SQLException {
        // TODO: Implement
        //list of all exercises
        String query = "SELECT * FROM Exercise ORDER BY ExerciseId";
        Statement statement = db.createStatement();
        ResultSet statementResults = statement.executeQuery(query);
        List<Exercise> exercises = new ArrayList<Exercise>();
        // creates an Exercise for every exercise in the database
        while (statementResults.next()) {
            exercises.add(createExercise(statementResults));
        }
        return exercises;
    }

    // ========== Submission Storage ===============

    /**
     * Store a submission in the database.
     * The id field of the submission will be ignored if it is -1.
     * <p>
     * Return -1 if the corresponding user doesn't exist in the database.
     *
     * @param submission
     * @return the submission id.
     * @throws SQLException
     */

    public int storeSubmission(Submission submission) throws SQLException {
        // TODO: Implement
        String query1 = "SELECT UserId FROM User WHERE Username =?;";
        PreparedStatement statement1 = db.prepareStatement(query1);
        statement1.setString(1, submission.user.username);
        ResultSet resultSet = statement1.executeQuery();
        String userIdName = "UserId";
        int userId = resultSet.getInt(userIdName);
        if (userId == -1) {
            return -1;
        }
        String query2 = "INSERT INTO Submission (SubmissionId, UserId, ExerciseId, SubmissionTime) VALUES (?,?,?,?);";
        PreparedStatement statement2 = db.prepareStatement(query2);
        setStatementParameters(statement2, userId, submission);
        return statement2.getGeneratedKeys().getInt(1);
    }
    // ============= Submission Query ===============


    /**
     * Return a prepared SQL statement that, when executed, will
     * return one row for every question of the latest submission for the given exercise by the given user.
     * <p>
     * The rows should be sorted by QuestionId, and each row should contain:
     * - A column named "SubmissionId" with the submission id.
     * - A column named "QuestionId" with the question id,
     * - A column named "Grade" with the grade for that question.
     * - A column named "SubmissionTime" with the time of submission.
     * <p>
     * Parameter 1 of the prepared statement will be set to the User's username, Parameter 2 to the Exercise Id, and
     * Parameter 3 to the number of questions in the given exercise.
     * <p>
     * This will be used by {@link #getLastSubmission(User, Exercise)}
     *
     * @return
     */
    PreparedStatement getLastSubmissionGradesStatement() throws SQLException {
        // TODO: Implement
        return db.prepareStatement(
                "SELECT  QuestionGrade.QuestionId, QuestionGrade.Grade,Submission.SubmissionTime, Submission.SubmissionId " +
                        "FROM Submission INNER JOIN QuestionGrade ON " +
                        "QuestionGrade.SubmissionId = Submission.SubmissionId INNER JOIN User ON " +
                        "Submission.UserId = User.UserId WHERE User.Username=? AND Submission.ExerciseId=?" +
                        "ORDER BY Submission.SubmissionTime DESC, QuestionId LIMIT ?;");
    }

    /**
     * Return a prepared SQL statement that, when executed, will
     * return one row for every question of the <i>best</i> submission for the given exercise by the given user.
     * The best submission is the one whose point total is maximal.
     * <p>
     * The rows should be sorted by QuestionId, and each row should contain:
     * - A column named "SubmissionId" with the submission id.
     * - A column named "QuestionId" with the question id,
     * - A column named "Grade" with the grade for that question.
     * - A column named "SubmissionTime" with the time of submission.
     * <p>
     * Parameter 1 of the prepared statement will be set to the User's username, Parameter 2 to the Exercise Id, and
     * Parameter 3 to the number of questions in the given exercise.
     * <p>
     * This will be used by {@link #getBestSubmission(User, Exercise)}
     *
     */
    PreparedStatement getBestSubmissionGradesStatement() throws SQLException {
        // TODO: Implement
        return null;
    }

    /**
     * Return a submission for the given exercise by the given user that satisfies
     * some condition (as defined by an SQL prepared statement).
     * <p>
     * The prepared statement should accept the user name as parameter 1, the exercise id as parameter 2 and a limit on the
     * number of rows returned as parameter 3, and return a row for each question corresponding to the submission, sorted by questionId.
     * <p>
     * Return null if the user has not submitted the exercise (or is not in the database).
     *
     * @param user
     * @param exercise
     * @param stmt
     * @return
     * @throws SQLException
     */
    Submission getSubmission(User user, Exercise exercise, PreparedStatement stmt) throws SQLException {
        stmt.setString(1, user.username);
        stmt.setInt(2, exercise.id);
        stmt.setInt(3, exercise.questions.size());

        ResultSet res = stmt.executeQuery();

        boolean hasNext = res.next();
        if (!hasNext)
            return null;

        int sid = res.getInt("SubmissionId");
        Date submissionTime = new Date(res.getLong("SubmissionTime"));

        float[] grades = new float[exercise.questions.size()];

        for (int i = 0; hasNext; ++i, hasNext = res.next()) {
            grades[i] = res.getFloat("Grade");
        }

        return new Submission(sid, user, exercise, submissionTime, (float[]) grades);
    }

    /**
     * Return the latest submission for the given exercise by the given user.
     * <p>
     * Return null if the user has not submitted the exercise (or is not in the database).
     *
     * @param user
     * @param exercise
     * @return
     * @throws SQLException
     */
    public Submission getLastSubmission(User user, Exercise exercise) throws SQLException {
        return getSubmission(user, exercise, getLastSubmissionGradesStatement());
    }


    /**
     * Return the submission with the highest total grade
     *
     * @param user the user for which we retrieve the best submission
     * @param exercise the exercise for which we retrieve the best submission
     * @return
     * @throws SQLException
     */
    public Submission getBestSubmission(User user, Exercise exercise) throws SQLException {
        return getSubmission(user, exercise, getBestSubmissionGradesStatement());
    }



    //ADDITIONAL METHODS + FUNCTIONS:
    private void createAndExecute(String sql) throws SQLException {
        Statement st = null;
        st = db.createStatement();
        st.executeUpdate(sql);
        st.close();
    }
    private void copyValues(PreparedStatement authorizedStatement,String sqlQuery, User user, String password) throws SQLException {
        authorizedStatement.setString(1, user.username);
        authorizedStatement.setString(2, user.firstname);
        authorizedStatement.setString(3, user.lastname);
        authorizedStatement.setString(4, password);
    }

    private void setStatementParameters(PreparedStatement statement2, int userId ,Submission submission) throws SQLException {
        // The id field of submission is ignored if submission.id is -1.
        if (submission.id != -1) {
            statement2.setInt(1, submission.id);
        }
        statement2.setInt(2, userId);
        statement2.setInt(3, submission.exercise.id);
        statement2.setLong(4, submission.submissionTime.getTime());
        storeQuestionsGrades(submission);
        statement2.execute();
    }
    private void storeQuestionsGrades(Submission submission) throws SQLException {
        List<Integer> questionsID = getQuestionsID(submission.exercise.id);
        String sql = "INSERT INTO QuestionGrade (SubmissionId, QuestionId, Grade) VALUES (?,?,?);";
        PreparedStatement storeQuestions = this.db.prepareStatement(sql);
        setQuestionParameters(storeQuestions, submission, questionsID);

    }
    private void setQuestionParameters(PreparedStatement storeQuestions, Submission submission, List<Integer> questionsID) throws SQLException {
        storeQuestions.setInt(1, submission.id);
        for (int i = 0; i < questionsID.size(); i++) {
            storeQuestions.setInt(2, questionsID.get(i));
            storeQuestions.setFloat(3, submission.questionGrades[i]);
            storeQuestions.execute();
        }
    }

    private List<Integer> getQuestionsID(int exerciseID) throws SQLException {
        List<Integer> questionIdsOfExercise = new ArrayList<>();
        String query = "SELECT QuestionId FROM Question WHERE ExerciseId =?;";
        PreparedStatement statement = this.db.prepareStatement(query);
        statement.setInt(1, exerciseID);
        ResultSet exerciseQuestions = statement.executeQuery();
        while (exerciseQuestions.next()) {
            questionIdsOfExercise.add(exerciseQuestions.getInt("QuestionId"));
        }
        return questionIdsOfExercise;
    }
    private Exercise createExercise(ResultSet statementResults) throws SQLException {
        int id = statementResults.getInt("ExerciseId");
        String name = statementResults.getString("Name");
        Date date = new Date(statementResults.getLong("DueDate"));
        Exercise resExercise = new Exercise(id, name, date);

        // find all questions related to the exercise, and add them to new Exercise
        String query = "SELECT * FROM Question WHERE ExerciseId=" + id + ";";
        Statement statement = db.createStatement();
        ResultSet resultSetQuestion = statement.executeQuery(query);
        while (resultSetQuestion.next()) {
            resExercise.addQuestion(
                    resultSetQuestion.getString("Name"),
                    resultSetQuestion.getString("Desc"),
                    resultSetQuestion.getInt("Points"));
        }
        return resExercise;
    }

}
