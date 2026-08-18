package com.geolink.findme.authservice.service.userService;

import com.geolink.findme.authservice.dto.mapper.UserMapper;
import com.geolink.findme.authservice.dto.request.UpdateProfileRequestDTO;
import com.geolink.findme.authservice.dto.response.UserProfileDTO;
import com.geolink.findme.authservice.entity.User;
import com.geolink.findme.authservice.exception.UserNotFoundException;
import com.geolink.findme.authservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service utilisateur.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'email: " + email));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserProfileDTO updateProfile(String email, UpdateProfileRequestDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'email: " + email));

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());

        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }
}
