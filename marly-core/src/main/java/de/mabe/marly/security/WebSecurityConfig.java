package de.mabe.marly.security;

import de.mabe.marly.user.UserService;

import java.security.Principal;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.context.WebApplicationContext;

@Configuration
@EnableOAuth2Sso
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
  private static final Logger log = Logger.getLogger(WebSecurityConfig.class);

  @Autowired private UserService userService;

  @Override
  protected void configure(HttpSecurity http) throws Exception {// @formatter:off
    http.csrf().disable();
    
    http
        .antMatcher("/**")
          .authorizeRequests()
            .antMatchers("/", "/index**", "/webjars/**", "/user", "/login")
              .permitAll()
            .antMatchers("/intern/**", "/info")
              .permitAll()  // ---> internal access only
            .anyRequest()
              .authenticated();
  } // @formatter:on

  @Bean
  public AuthoritiesExtractor authoritiesExtractor(UserService userService) {
    return map -> {
      String email = "" + map.get("email");
      return userService.isAdmin(email)
          ? AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN")
          : AuthorityUtils.createAuthorityList("ROLE_USER");
    };
  }

  @EventListener
  public void onAuthenticationSucceeded(AuthenticationSuccessEvent event) {
    OAuth2Authentication authentication = (OAuth2Authentication) event.getSource();
    Authentication userAuthentication = authentication.getUserAuthentication();
    Map<String, String> details = (Map<String, String>) userAuthentication.getDetails();
    String email = details.get("email");

    log.info("Login successful -> " + email + " (" + userAuthentication.getAuthorities() + ")");

    userService.createUserIfNotExist(email);
  }


}
