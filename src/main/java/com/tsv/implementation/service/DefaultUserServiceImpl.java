package com.tsv.implementation.service;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.tsv.implementation.config.TwilioConfig;
import com.tsv.implementation.dao.RoleRepository;
import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.dto.UserRegisteredDTO;
import com.tsv.implementation.model.Role;
import com.tsv.implementation.model.User;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;


@Service
public class DefaultUserServiceImpl implements DefaultUserService{
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
  	private RoleRepository roleRepo;
  	
	@Autowired
	private TwilioConfig twilioConfig;
   
	private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
	
		User user = userRepo.findByEmail(email);
		if(user == null) {
			throw new UsernameNotFoundException("Invalid username or password.");
		}
		return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), mapRolesToAuthorities(user.getRole()));		
	}
	
	private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Role> roles){
		return roles.stream().map(role -> new SimpleGrantedAuthority(role.getRole())).collect(Collectors.toList());
	}

	@Override
	public User save(UserRegisteredDTO userRegisteredDTO) {
		Role role = roleRepo.findByRole("USER");
		
		User user = new User();
		user.setEmail(userRegisteredDTO.getEmail_id());
		user.setName(userRegisteredDTO.getName());
		user.setPhoneNumber(userRegisteredDTO.getPhoneNumber());
		user.setPassword(passwordEncoder.encode(userRegisteredDTO.getPassword()));
		user.setRole(role);
		
		return userRepo.save(user);
	}

	@Override
	public String generateOtp(User user) {
		try {
			PhoneNumber to = new PhoneNumber(user.getPhoneNumber());
			PhoneNumber from = new PhoneNumber(twilioConfig.getTrialNumber());
			
			String otp = this.generateOTP();
			user.setActive(false);
			user.setOtp(otp);
			this.userRepo.save(user);
			
			String otpMessage = "Dear Customer, Your OTP is ## "+otp+" ##. Use this Passcode to complete your transaction. Thank You.";
			Message message = Message.creator(to, from, otpMessage).create();
			
			return "success";
		}catch (Exception e) {
			e.printStackTrace();
			return "error";
		}
	}
	
	private String generateOTP() {
		return new DecimalFormat("000000")
				.format(new Random().nextInt(999999));
	}

}
