package jtechlog.enverstutorial;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.*;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class Employee implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedAt;

    @OneToMany(mappedBy="employee", cascade=CascadeType.ALL)
    private List<Phone> phones = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void updateModifiedAt() {
        modifiedAt = new Date();
    }

    public Employee() {
    }

    public Employee(String name) {
        this.name = name;
    }

    public void addPhone(Phone phone) {
        phones.add(phone);
        phone.setEmployee(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Phone> getPhones() {
        return phones;
    }

    public void setPhones(List<Phone> phones) {
        this.phones = phones;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
