package br.com.redemob.validador.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record DadosInformados(String nome, LocalDate dataNascimento, String cpf) {

	private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	public static DadosInformados vazio() {
		return new DadosInformados("", null, "");
	}

	public String dataNascimentoFormatada() {
		return this.dataNascimento == null ? "" : this.dataNascimento.format(DATA_BR);
	}

}
