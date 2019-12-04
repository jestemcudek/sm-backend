package pl.edu.agh.kis.smbackend.dao;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.kis.smbackend.model.User;

import java.util.List;

@Repository
public interface UserDAO extends JpaRepository<User, Integer> {

    User getUserByEmailAndAndPassword(String email, String password);

    User getUserByEmail(String email);

    List<User> findAllByAndEmail(List<String> emails);

}
