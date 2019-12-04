package pl.edu.agh.kis.smbackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.edu.agh.kis.smbackend.dao.GroupDAO;
import pl.edu.agh.kis.smbackend.dao.LocationDAO;
import pl.edu.agh.kis.smbackend.dao.UserDAO;
import pl.edu.agh.kis.smbackend.model.Group;
import pl.edu.agh.kis.smbackend.model.User;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SignMapController {

    @Autowired
    UserDAO userDAO;

    @Autowired
    LocationDAO locationDAO;

    @Autowired
    GroupDAO groupDAO;

    @PostMapping(path = "/register", produces = "application/json")
    public ResponseEntity<User> registerUser(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password) {
        User userToAdd = new User(email, firstName, lastName, password);
        userDAO.saveAndFlush(userToAdd);
        URI uri =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(userToAdd.getId())
                        .toUri();
        return ResponseEntity.created(uri).header("Access-Control-Allow-Origin", "*").body(userToAdd);
    }

    @PostMapping(path = "/login", produces = "application/json")
    public ResponseEntity<User> loginUser(@RequestParam String email, @RequestParam String password) {
        User loggedUser = userDAO.getUserByEmailAndAndPassword(email, password);
        if (loggedUser == null) {
            return ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();
        }
        return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").body(loggedUser);
    }

    @PostMapping(path = "/forgotten", produces = "application/json")
    public ResponseEntity<String> remindPassword(@RequestParam String email) {
        String userPwd = userDAO.getUserByEmail(email).getPassword();
        return (userPwd != null)
                ? ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").body(userPwd)
                : ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();
    }

    @PostMapping(path = "/password", produces = "application/json")
    public ResponseEntity<String> changePassword(
            @RequestParam String newpass, @RequestParam String oldPass, @RequestParam int userID) {
        User userToChange =
                userDAO.findById(userID).filter(user -> user.getPassword().equals(oldPass)).get();
        if (userToChange != null) {
            userToChange.setPassword(newpass);
            return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").build();
        } else return ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();
    }

    @PostMapping(path = "/group", produces = "application/json")
    public ResponseEntity<Group> createGroup(
            @RequestParam String name, @RequestParam List<String> usersMails) {
        List<User> users = new ArrayList<>();
        users.addAll(userDAO.findAllByeAndEmail(usersMails));
        Group newGroup = new Group(name, users);
        groupDAO.save(newGroup);
        URI uri =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{groupID")
                        .buildAndExpand(newGroup.getId())
                        .toUri();
        return ResponseEntity.created(uri).header("Access-Control-Allow-Origin", "*").body(newGroup);
    }

    @PostMapping(path = "/selectGroup", produces = "application/json")
    public ResponseEntity<Group> selectGroup(@RequestParam int userID, @RequestParam int groupID) {
        User newMember = null;
        if (userDAO.findById(userID).isPresent() && groupDAO.findById(groupID).isPresent()) {
            newMember = userDAO.findById(userID).get();
            groupDAO.findById(groupID).get().getGroupMembers().add(newMember);
            return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").build();
        } else return ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();
    }

    @PostMapping(path = "leaveGroup", produces = "application/json")
    public ResponseEntity<Group> leaveGroup(@RequestParam int userID, @RequestParam int groupID) {
        if (!userDAO.findById(userID).isPresent())
            return ResponseEntity.notFound().header("Access-Control-Allow-Origin", "*").build();

        groupDAO
                .findById(groupID)
                .ifPresent(group -> group.getGroupMembers().remove(userDAO.findById(userID).get()));
        return ResponseEntity.ok().header("Access-Control-Allow-Origin", "*").build();
    }

    @GetMapping(path = "/groups", produces = "application/json")
    public ResponseEntity<List<Group>> getGroupsByUser(@RequestParam int userID) {
        if (!userDAO.findById(userID).isPresent()) return ResponseEntity.notFound().build();
        List<Group> groups =
                groupDAO.findAll().stream()
                        .filter(
                                group -> group.getGroupMembers().stream().anyMatch(user -> user.getId() == userID))
                        .collect(Collectors.toList());
        return ResponseEntity.ok(groups);
    }
}
