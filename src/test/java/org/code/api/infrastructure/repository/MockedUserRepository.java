package org.code.api.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.code.api.domain.models.user.User;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

public class MockedUserRepository implements UserRepository{
  @Override
  public void flush() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'flush'");
  }

  @Override
  public <S extends User> S saveAndFlush(S entity) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'saveAndFlush'");
  }

  @Override
  public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'saveAllAndFlush'");
  }

  @Override
  public void deleteAllInBatch(Iterable<User> entities) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteAllInBatch'");
  }

  @Override
  public void deleteAllByIdInBatch(Iterable<UUID> ids) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteAllByIdInBatch'");
  }

  @Override
  public void deleteAllInBatch() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteAllInBatch'");
  }

  @Override
  public User getOne(UUID id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getOne'");
  }

  @Override
  public User getById(UUID id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getById'");
  }

  @Override
  public User getReferenceById(UUID id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getReferenceById'");
  }

  @Override
  public <S extends User> List<S> findAll(Example<S> example) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findAll'");
  }

  @Override
  public <S extends User> List<S> findAll(Example<S> example, Sort sort) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findAll'");
  }

  @Override
  public <S extends User> List<S> saveAll(Iterable<S> entities) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'saveAll'");
  }

  @Override
  public List<User> findAll() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findAll'");
  }

  @Override
  public List<User> findAllById(Iterable<UUID> ids) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findAllById'");
  }

  @Override
  public <S extends User> S save(S entity) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'save'");
  }

  @Override
  public Optional<User> findById(UUID id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findById'");
  }

  @Override
  public boolean existsById(UUID id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'existsById'");
  }

  @Override
  public long count() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'count'");
  }

  @Override
  public void deleteById(UUID id) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
  }

  @Override
  public void delete(User entity) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'delete'");
  }

  @Override
  public void deleteAllById(Iterable<? extends UUID> ids) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteAllById'");
  }

  @Override
  public void deleteAll(Iterable<? extends User> entities) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteAll'");
  }

  @Override
  public void deleteAll() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteAll'");
  }

  @Override
  public List<User> findAll(Sort sort) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findAll'");
  }

  @Override
  public Page<User> findAll(Pageable pageable) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findAll'");
  }

  @Override
  public <S extends User> Optional<S> findOne(Example<S> example) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findOne'");
  }

  @Override
  public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findAll'");
  }

  @Override
  public <S extends User> long count(Example<S> example) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'count'");
  }

  @Override
  public <S extends User> boolean exists(Example<S> example) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'exists'");
  }

  @Override
  public <S extends User, R> R findBy(Example<S> example, Function<FetchableFluentQuery<S>, R> queryFunction) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findBy'");
  }

  @Override
  public Optional<User> findByEmail(String email) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findByEmail'");
  }

  @Override
  public boolean existsByEmail(String email) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'existsByEmail'");
  }
  
}
