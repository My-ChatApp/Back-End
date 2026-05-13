package iuh.fit.userservice.repository;

import iuh.fit.userservice.entity.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    UserProfile findByUserId(String userId);
}
