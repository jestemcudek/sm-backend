package pl.edu.agh.kis.smbackend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.kis.smbackend.model.Group;

@Repository
public interface GroupDAO extends JpaRepository<Group, Integer> {}
