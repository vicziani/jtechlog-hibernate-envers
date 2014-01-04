package jtechlog.enverstutorial;

import java.util.List;
import javax.persistence.EntityManager;

public class EmployeeService {

    private EntityManager em;

    public void persistEmployee(Employee employee) {
        em.persist(employee);
    }

    public void mergeEmployee(Employee employee) {
        em.merge(employee);
    }

    public void removeEmployee(long id) {
        Employee employee = em.find(Employee.class, id);
        em.remove(employee);
    }

    public List<Employee> listEmployees(int firstResult, int maxResults) {
        return em.createQuery("select e from Employee e join fetch e.phones", Employee.class)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }
}
