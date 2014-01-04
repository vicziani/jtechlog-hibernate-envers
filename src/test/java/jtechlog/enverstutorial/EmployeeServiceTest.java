package jtechlog.enverstutorial;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import java.util.Date;
import java.util.List;
import org.apache.commons.dbutils.handlers.MapListHandler;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import org.junit.BeforeClass;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.hibernate.envers.RevisionType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 */
public class EmployeeServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeServiceTest.class);
    private static EntityManagerFactory emf;
    private EntityManager em;
    private Connection conn;
    EmployeeService employeeService;

    @BeforeClass
    public static void initResources() throws ClassNotFoundException {
        LOGGER.info("Driver inicializalasa");
        Class.forName("org.hsqldb.jdbc.JDBCDriver");
        LOGGER.info("EntityManagerFactory letrehozasa");
        // Táblák létrehozása
        emf = Persistence.createEntityManagerFactory("enversTutorialPu");
    }

    @Before
    public void setUp() throws Exception {
        em = emf.createEntityManager();
        conn = DriverManager.getConnection("jdbc:hsqldb:mem:enverstutor", "SA", "");
        employeeService = new EmployeeService();
        employeeService.setEm(em);
        // Táblák törlése
        QueryRunner runner = new QueryRunner();
        runner.update(conn, "delete from Employee");
        runner.update(conn, "delete from Employee_AUD");
        runner.update(conn, "delete from Phone");
        runner.update(conn, "delete from Phone_AUD");
        runner.update(conn, "delete from REVINFO");
        runner.update(conn, "alter table REVINFO alter column REV restart with 1");
    }

    @After
    public void tearDown() throws SQLException {
        LOGGER.info("EntityManagerFactory lezarasa");
        em.close();
        LOGGER.info("Connections lezarasa");
        DbUtils.close(conn);

    }

    @AfterClass
    public static void closeResources() {
        emf.close();
    }

    @Test
    public void testAuditedAdd() throws SQLException {
        Employee employee = new Employee("name1");
        employee.getPhones().add(new Phone("home", "1234567"));

        em.getTransaction().begin();
        em.persist(employee);
        em.getTransaction().commit();

        QueryRunner runner = new QueryRunner();
        Map result = runner.query(conn, "select count(*) as cnt from revinfo", new MapHandler());
        assertEquals(1l, result.get("cnt"));

        List<Map<String, Object>> results = runner.query(conn, "select *  from Employee_AUD order by rev", new MapListHandler());
        assertEquals(1, results.size());
        // Hozzáadott
        assertEquals("name1", results.get(0).get("name"));
        // revtype == 0: ADD
        assertEquals(0, results.get(0).get("revtype"));
    }

    @Test
    public void testAuditedMerge() throws SQLException {
        Employee employee = new Employee("name1");
        employee.getPhones().add(new Phone("home", "1234567"));

        em.getTransaction().begin();
        em.persist(employee);
        em.getTransaction().commit();

        employee.setName("name2");

        em.getTransaction().begin();
        em.merge(employee);
        em.getTransaction().commit();

        QueryRunner runner = new QueryRunner();
        Map result = runner.query(conn, "select count(*) as cnt from revinfo", new MapHandler());
        assertEquals(2l, result.get("cnt"));

        List<Map<String, Object>> results = runner.query(conn, "select *  from Employee_AUD order by rev", new MapListHandler());
        // Módosított
        assertEquals(2, results.get(1).get("rev"));
        assertEquals("name2", results.get(1).get("name"));
        // revtype == 1: MOD
        assertEquals(1, results.get(1).get("revtype"));

    }

    @Test
    public void testAuditedRemove() throws SQLException {
        Employee employee = new Employee("name1");
        employee.getPhones().add(new Phone("home", "1234567"));

        em.getTransaction().begin();
        em.persist(employee);
        em.getTransaction().commit();

        em.getTransaction().begin();
        em.remove(employee);
        em.getTransaction().commit();

        QueryRunner runner = new QueryRunner();
        Map result = runner.query(conn, "select count(*) as cnt from revinfo", new MapHandler());
        assertEquals(2l, result.get("cnt"));

        List<Map<String, Object>> results = runner.query(conn, "select *  from Employee_AUD order by rev", new MapListHandler());
        assertEquals(2, results.size());

        // Törölt
        // revtype == 2: DEL
        assertEquals(2, results.get(1).get("rev"));
        assertEquals(2, results.get(1).get("revtype"));
    }

    @Test
    public void testAuditedInterceptor() throws SQLException {
        Employee employee = new Employee("name1");
        employee.getPhones().add(new Phone("home", "1234567"));

        em.getTransaction().begin();
        em.persist(employee);
        em.getTransaction().commit();
        Date d = employee.getModifiedAt();

        employee.setName("name2");

        em.getTransaction().begin();
        em.merge(employee);
        em.getTransaction().commit();
        Date d2 = employee.getModifiedAt();

        QueryRunner runner = new QueryRunner();
        List<Map<String, Object>> results = runner.query(conn, "select modifiedAt from Employee_AUD order by rev", new MapListHandler());
        assertEquals(d.getTime(), ((Date) results.get(0).get("modifiedAt")).getTime());

        assertEquals(d2.getTime(), ((Date) results.get(1).get("modifiedAt")).getTime());
    }

    @Test
    public void testForRevisionsOfEntity() throws SQLException {
        Employee employee = new Employee("name1");
        employee.getPhones().add(new Phone("home", "1234567"));

        em.getTransaction().begin();
        em.persist(employee);
        em.getTransaction().commit();

        employee.setName("name2");

        em.getTransaction().begin();
        em.merge(employee);
        em.getTransaction().commit();

        AuditReader auditReader = AuditReaderFactory.get(em);
        List revisions = auditReader.createQuery().forRevisionsOfEntity(Employee.class, false, true).getResultList();
        assertEquals(2, revisions.size());

        assertEquals("name1", ((Employee)((Object[])revisions.get(0))[0]).getName());
        assertEquals(1, ((DefaultRevisionEntity) ((Object[])revisions.get(0))[1]).getId());
        assertEquals(RevisionType.ADD, ((Object[])revisions.get(0))[2]);
    }

    @Test
    public void testForEntitiesAtRevision() throws SQLException {
        Employee employee = new Employee("name1");
        employee.getPhones().add(new Phone("home", "1234567"));

        em.getTransaction().begin();
        em.persist(employee);
        em.getTransaction().commit();

        employee.setName("name2");

        em.getTransaction().begin();
        em.merge(employee);
        em.getTransaction().commit();

        AuditReader auditReader = AuditReaderFactory.get(em);
        Employee revision = (Employee) auditReader.createQuery().forEntitiesAtRevision(Employee.class, 1).getSingleResult();

        assertEquals("name1", revision.getName());
    }
}
