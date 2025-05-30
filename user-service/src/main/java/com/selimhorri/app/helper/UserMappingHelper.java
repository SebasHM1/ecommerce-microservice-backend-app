package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;

public interface UserMappingHelper {
	
	public static UserDto map(final User user) {

		System.out.println("UserMappingHelper.map - Entrando para user ID: " + (user != null ? user.getUserId() : "USER ES NULL"));

    if (user == null) return null; // Guarda por si acaso

    Credential credentialEntity = user.getCredential();

    if (credentialEntity == null) {
        System.out.println("UserMappingHelper.map - user.getCredential() ES NULL para User ID: " + user.getUserId());
    } else {
        System.out.println("UserMappingHelper.map - user.getCredential() NO ES NULL para User ID: " + user.getUserId());
        System.out.println("  Credential Entity Object: " + credentialEntity.toString()); // Puede ser útil
        System.out.println("  Credential ID: " + credentialEntity.getCredentialId());
        System.out.println("  Username: " + credentialEntity.getUsername());
        System.out.println("  Password Hash (solo longitud si es sensible): " + (credentialEntity.getPassword() != null ? credentialEntity.getPassword().length() : "null"));
        System.out.println("  Role: " + credentialEntity.getRoleBasedAuthority());
        System.out.println("  isEnabled: " + credentialEntity.getIsEnabled());
        System.out.println("  isAccountNonExpired: " + credentialEntity.getIsAccountNonExpired());
        System.out.println("  isAccountNonLocked: " + credentialEntity.getIsAccountNonLocked());
        System.out.println("  isCredentialsNonExpired: " + credentialEntity.getIsCredentialsNonExpired());
    }

		return UserDto.builder()
				.userId(user.getUserId())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.imageUrl(user.getImageUrl())
				.email(user.getEmail())
				.phone(user.getPhone())
				.credentialDto( user.getCredential() != null ?
						CredentialDto.builder()
							.credentialId(user.getCredential().getCredentialId())
							.username(user.getCredential().getUsername())
							.password(user.getCredential().getPassword())
							.roleBasedAuthority(user.getCredential().getRoleBasedAuthority())
							.isEnabled(user.getCredential().getIsEnabled())
							.isAccountNonExpired(user.getCredential().getIsAccountNonExpired())
							.isAccountNonLocked(user.getCredential().getIsAccountNonLocked())
							.isCredentialsNonExpired(user.getCredential().getIsCredentialsNonExpired())
							.build() : null)
				.build();
	}
	
	public static User map(final UserDto userDto) {
		return User.builder()
				.userId(userDto.getUserId())
				.firstName(userDto.getFirstName())
				.lastName(userDto.getLastName())
				.imageUrl(userDto.getImageUrl())
				.email(userDto.getEmail())
				.phone(userDto.getPhone())
				.credential( userDto.getCredentialDto() != null ?
						Credential.builder()
							.credentialId(userDto.getCredentialDto().getCredentialId())
							.username(userDto.getCredentialDto().getUsername())
							.password(userDto.getCredentialDto().getPassword())
							.roleBasedAuthority(userDto.getCredentialDto().getRoleBasedAuthority())
							.isEnabled(userDto.getCredentialDto().getIsEnabled())
							.isAccountNonExpired(userDto.getCredentialDto().getIsAccountNonExpired())
							.isAccountNonLocked(userDto.getCredentialDto().getIsAccountNonLocked())
							.isCredentialsNonExpired(userDto.getCredentialDto().getIsCredentialsNonExpired())
							.build() : null)
				.build();
	}
	
	
	
}
