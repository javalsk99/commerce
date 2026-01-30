package lsk.commerce.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.EnumType.*;
import static lombok.AccessLevel.*;
import static lsk.commerce.domain.Grade.*;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

    private String name;
    private String loginId;
    private String password;

    @Enumerated(STRING)
    private Grade grade;

    @Embedded
    private Address address;

    public Member(String name, String loginId, String password, String city, String street, String zipcode) {
        this.name = name;
        this.loginId = loginId;
        this.password = password;
        this.grade = USER;
        this.address = new Address(city, street, zipcode);
    }

    protected void addOrder(Order order) {
        orders.add(order);
        order.setMember(this);
    }

    public void setAdmin() {
        this.grade = ADMIN;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void changeAddress(String newCity, String newStreet, String newZipcode) {
        this.address = new Address(newCity, newStreet, newZipcode);
    }
}
