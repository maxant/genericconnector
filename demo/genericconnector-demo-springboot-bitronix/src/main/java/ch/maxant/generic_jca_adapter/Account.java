package ch.maxant.generic_jca_adapter;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="JEE_PERSON")
public class Account {

	@Id
	@Column(name="ID")
	private Long id;

	@Temporal(TemporalType.DATE)
	@Column(name="DOB", nullable = false)
	private Date dob;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

}
