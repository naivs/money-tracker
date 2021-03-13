package org.naivs.moneytracker.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import org.naivs.moneytracker.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@ToString
@EqualsAndHashCode
public class Transaction {

	@EqualsAndHashCode.Exclude
	private Integer hash;

	private BigDecimal amount;

	private TransactionType type;
	private String note;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime timestamp;
}
