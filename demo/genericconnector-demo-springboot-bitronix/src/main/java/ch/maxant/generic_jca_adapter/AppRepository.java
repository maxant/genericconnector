package ch.maxant.generic_jca_adapter;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRepository extends JpaRepository<Account, Long> {
}
