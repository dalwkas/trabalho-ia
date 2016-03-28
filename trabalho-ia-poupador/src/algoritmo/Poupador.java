package algoritmo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import controle.Constantes;

public class Poupador extends ProgramaPoupador {

	// Constantes da matriz de visão
	private static final int semVisaoParaLocal = -2;
	private static final int foraDoAmbiente = -1;
	private static final int parede = 1;
	private static final int banco = 3;
	private static final int moeda = 4;
	private static final int pastinhaDoPoder = 5;
	//Fim das constantes
	
	private int[] visao;
	private int[] pesos;
	private ArrayList<Integer> esquerda;
	private ArrayList<Integer> direita;
	private ArrayList<Integer> cima;
	private ArrayList<Integer> baixo;
	private ArrayList<Integer> aoRedor;
	private HashMap<Point, Integer> pontosVisitados;
	private int tempoSemColetarMoedas;
	private HashMap<String, Integer> mapa;
	private Point pontoAnterior;
	private int acaoAnterior;
	private ArrayList<Integer> acoesAnteriores;
	private int moedasAnteriores;


	public Poupador() {
		acoesAnteriores = new ArrayList<Integer>();
		mapa = new HashMap<String, Integer>();
		this.pontosVisitados = new HashMap<Point, Integer>();
		aoRedor = new ArrayList<Integer>(Arrays.asList(6, 7, 8, 11, 12, 15, 16, 17));
		cima = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
		baixo = new ArrayList<Integer>(Arrays.asList(14, 15, 16, 17, 18, 19, 20, 21, 22, 23));
		esquerda = new ArrayList<Integer>(Arrays.asList(10, 11, 0, 1, 5, 6, 14, 15, 19, 20));
		direita = new ArrayList<Integer>(Arrays.asList(12, 13, 3, 4, 8, 9, 17, 18, 22, 23));

	}

	public int acao() {
		this.pesos = new int[24];
		reduzirTempoMapa();
		pesoPontoAtual(sensor.getPosicao());
		analisarPontosJaVisitados();
		analisarVisao();
		analisarOlfato(sensor.getAmbienteOlfatoLadrao(), true);
		analisarOlfato(sensor.getAmbienteOlfatoPoupador(), false);
		return decidirMovimento();
	}

	// Reduz o peso para o ponto atual
	private void pesoPontoAtual(Point p) {
		if (pontosVisitados.containsKey(p)) {
			int peso = pontosVisitados.get(p);
			peso -= 20;
			pontosVisitados.put(p, peso);
		} else {
			pontosVisitados.put(p, -20);
		}
		mapa.put(sensor.getPosicao().x + "" + sensor.getPosicao().y, 10);
	}

	// Torna mais improvavel a visitacao de pontos ja visitados
	private void analisarPontosJaVisitados() {
		Point ponto = sensor.getPosicao();
		Point esquerda = new Point(ponto.x - 1, ponto.y);
		Point direita = new Point(ponto.x + 1, ponto.y);
		Point cima = new Point(ponto.x, ponto.y - 1);
		Point baixo = new Point(ponto.x, ponto.y + 1);
		pesos[7] += (mapa.get(cima.x + "" + cima.y) == null ? 0 : -20 * mapa.get(cima.x + "" + cima.y));
		pesos[12] += (mapa.get(direita.x + "" + cima.y) == null ? 0 : -20 * mapa.get(direita.x + "" + cima.y));
		pesos[16] += (mapa.get(baixo.x + "" + baixo.y) == null ? 0 : -20 * mapa.get(baixo.x + "" + baixo.y));
		pesos[11] += (mapa.get(esquerda.x + "" + esquerda.y) == null ? 0
				: -20 * mapa.get(esquerda.x + "" + esquerda.y));

	}
	
	public int decidirMovimento() {

		if ((moedasAnteriores == sensor.getNumeroDeMoedas()) && (sensor.getNumeroDeMoedas() > 0)) {
			tempoSemColetarMoedas++;
		} else {
			tempoSemColetarMoedas = 0;
		}

		// Avaliar se o banco esta proximo
		Point banco = Constantes.posicaoBanco;
		if (banco.x < sensor.getPosicao().x) {
			pesos[11] += sensor.getNumeroDeMoedas() * (40 + tempoSemColetarMoedas);
		}

		if (banco.x > sensor.getPosicao().x) {
			pesos[12] += sensor.getNumeroDeMoedas() * (40 + tempoSemColetarMoedas);
		}

		if (banco.y < sensor.getPosicao().y) {
			pesos[7] += sensor.getNumeroDeMoedas() * (40 + tempoSemColetarMoedas);
		}

		if (banco.y > sensor.getPosicao().y) {
			pesos[16] += sensor.getNumeroDeMoedas() * (40 + tempoSemColetarMoedas);
		}

		// Diminuir os pesos para os 4 movimentos

		int pesoEsquerda = somarPesos(esquerda) + pesoParedePastinhaBanco(11);
		int pesoDireita = somarPesos(direita) + pesoParedePastinhaBanco(12);
		int pesoCima = somarPesos(cima) + pesoParedePastinhaBanco(7);
		int pesoBaixo = somarPesos(baixo) + pesoParedePastinhaBanco(16);

		int[] pesosDirecao = { pesoCima, pesoBaixo, pesoDireita, pesoEsquerda };

		// Se acao foi ineficaz, reduzir peso

		if ((pontoAnterior != null) && (pontoAnterior.equals(sensor.getPosicao()))) {
			acoesAnteriores.add(acaoAnterior);
			for (int i : acoesAnteriores) {
				pesosDirecao[i - 1] += -3500;
			}

		} else {
			acoesAnteriores.clear();
		}

		int maiorPeso = -99999;
		int direcao = -1;

		for (int i = 0; i < pesosDirecao.length; i++) {
			if (pesosDirecao[i] > maiorPeso) {
				maiorPeso = pesosDirecao[i];
				direcao = i + 1;
			}
		}

		ArrayList<Integer> pesosIguais = new ArrayList<Integer>();
		// Verifica se existe mais de um maiorPeso
		for (int i = 0; i < pesosDirecao.length; i++) {
			if (pesosDirecao[i] == maiorPeso) {
				pesosIguais.add(i + 1);
			}
		}

		if (pesosIguais.size() > 1) {
			int indice = (int) (Math.random() * (pesosIguais.size()));
			direcao = pesosIguais.get(indice);

		}
		
		moedasAnteriores = sensor.getNumeroDeMoedas();
		pontoAnterior = sensor.getPosicao();
		acaoAnterior = direcao;
		

		/*
		 * Os valores para retornar são:
		 * 0 - ficar parado 
		 * 1 - mover para cima 
		 * 2 - mover para baixo 
		 * 3 - mover pra direita 
		 * 4 - mover pra esquerda
		 */

		if ((direcao < 1) || (direcao > 4)) {
			return 0;
		}
		return direcao;

	}
	
	private void reduzirTempoMapa() {
		List<String> removidos = new ArrayList<String>();
		for (Entry<String, Integer> entry : mapa.entrySet()) {
			entry.setValue(entry.getValue() - 1);
			if (entry.getValue() == 0) {
				removidos.add(entry.getKey());
			}
		}
		for (String string : removidos) {
			mapa.remove(string);
		}
	}

	public void analisarOlfato(int[] olfato, boolean ladrao) {
		if (ladrao) {
			for (int i = 0; i < olfato.length; i++) {
				this.pesos[aoRedor.get(i)] += (olfato[i] == 0) ? 600 : -600 * (3 - olfato[i]);
				if (sensor.getNumeroDeMoedas() == 0) {
					this.pesos[aoRedor.get(i)] += (1600 * (3 - olfato[i]));
				}
			}
		} else {
			for (int i = 0; i < olfato.length; i++) {
				this.pesos[aoRedor.get(i)] += (olfato[i] == 0) ? 3500 : (-300 * (3 - olfato[i]));
			}
		}

	}

	// Setar os pesos a partir da visao do agente

	public void analisarVisao() {

		visao = sensor.getVisaoIdentificacao();
		for (int i = 0; i < visao.length; i++) {
			switch (visao[i]) {
			case semVisaoParaLocal:
				this.pesos[i] += -50;
				break;
			case foraDoAmbiente:
				this.pesos[i] += -300;
				break;
			case parede:
				this.pesos[i] += -300;
				break;
			case banco:
				this.pesos[i] += 300 * (sensor.getNumeroDeMoedas());
				break;
			case moeda:
				this.pesos[i] += 1500;
				break;
			case pastinhaDoPoder:
				this.pesos[i] += -500;
			default:
				if (visao[i] >= 100) {
					// se for outro poupador ou ladrao

					if (sensor.getNumeroDeMoedas() == 0) {
						this.pesos[i] += 3000;
					} else {
						this.pesos[i] += -7000;
					}

				} else {
					this.pesos[i] += -3;
				}
				break;
			}

		}
	}

	
	private int pesoParedePastinhaBanco(int posicao) {

		if ((visao[posicao] == parede) || (visao[posicao] >= 50) || (visao[posicao] == foraDoAmbiente)) {
			return -3000;
		}
		if ((visao[posicao] == pastinhaDoPoder)) {
			return -3500;
		}
		if ((sensor.getNumeroDeMoedas() == 0) && (visao[posicao] == banco)) {
			return -3000;
		}

		return 0;

	}

	private int somarPesos(ArrayList<Integer> direcao) {
		int soma = 0;
		for (int i : direcao) {
			soma += pesos[i];
		}
		return soma;
	}
}