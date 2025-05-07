package com.example.journalservice.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl; // For the helper method

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (IllegalArgumentException e) {
                logger.error("Unable to get JWT Token", e);
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token has expired", e);
            } catch (SignatureException e) {
                logger.error("JWT signature does not match locally computed signature", e);
            } catch (MalformedJwtException e) {
                logger.error("JWT token is malformed", e);
            } catch (UnsupportedJwtException e) {
                logger.error("JWT token is unsupported", e);
            }
        } else {
            if (authorizationHeader != null) {
                logger.warn("JWT Token does not begin with Bearer String");
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Validate token (username extracted from token must match, and not expired)
            if (jwtUtil.validateToken(jwt, username)) { // Using the simpler validateToken(jwt, usernameFromToken)
                List<String> roles = jwtUtil.extractRoles(jwt);

                // Use the helper method from UserDetailsServiceImpl to build UserDetails
                UserDetails userDetails = userDetailsServiceImpl.buildUserDetailsFromJwt(username, roles);

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                logger.debug("Set SecurityContext for user '{}', authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());
            } else {
                logger.warn("JWT token validation failed for user: {}", username);
            }
        }
        chain.doFilter(request, response);
    }
}