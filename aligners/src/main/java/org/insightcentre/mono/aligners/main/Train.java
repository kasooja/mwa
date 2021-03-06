package org.insightcentre.mono.aligners.main;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.insightcentre.mono.aligners.Aligner;
import org.insightcentre.mono.aligners.AlignmentTrainer;
import org.insightcentre.mono.aligners.FeatureExtractor;
import org.insightcentre.mono.aligners.PrettyGoodTokenizer;
import org.insightcentre.mono.aligners.featextractors.GloveFeatureExtractor;
import org.insightcentre.mono.aligners.jacana.JacanaAligner;
import org.insightcentre.mono.aligners.nn.BiLSTMSimAligner;
import org.insightcentre.mono.aligners.nn.BiLSTMSimAlignmentTrainer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.insight.unlp.nn.utils.BasicFileTools;

/**
 *
 * @author Kartik Asooja
 */
public class Train {

	private final static double SCORE_THRESHOLD = 0.8;
	private final static String SCORE_THRESHOLD_STRING = "08_40000";
	private static int maxDocs = 40000;

	public static void main(String[] args) throws Exception {

		final List<AlignmentTrainer.TrainingPair> alignTraining = new ArrayList<>();
		final FeatureExtractor featureExtractor = new GloveFeatureExtractor();
		int docAdded = 0;

		// Read data 1
		final Collection<SentencePair> data = new ArrayList<SentencePair>();
		File[] files = new File("data").listFiles();
		for(File file : files){
			Gson gson = new Gson();
			Type type = new TypeToken<Collection<SentencePair>>(){}.getType();
			Collection<SentencePair> fileData = gson.fromJson(new FileReader(file), type);
			data.addAll(fileData);
		}

		JacanaAligner al = new JacanaAligner();
		
		// Extract features
		for(SentencePair datum : data) {
			if(((Double)datum.getSimScore()) > SCORE_THRESHOLD) {
				final String[] s1 = PrettyGoodTokenizer.tokenize(datum.getSentence1().toString());
				final String[] s2 = PrettyGoodTokenizer.tokenize(datum.getSentence2().toString());

				if(docAdded<maxDocs){
					alignTraining.add(new AlignmentTrainer.TrainingPair(
							featureExtractor.extractFeatures(s1, datum.getSentence1()),
							featureExtractor.extractFeatures(s2, datum.getSentence2()), datum.getSimScore()));
					al.align(featureExtractor.extractFeatures(s1, datum.getSentence1()), featureExtractor.extractFeatures(s2, datum.getSentence2()));
					
					docAdded++;
				}
			}
		}

		// Read data 4 and extract features 
		String data4Path = "sentData/sts2016-english";
		File[] files2 = new File(data4Path).listFiles();
		for(File file : files2){
			BufferedReader br = BasicFileTools.getBufferedReader(file.getAbsolutePath());
			String line = "";
			while((line=br.readLine()) != null){
				String[] split = line.split("\t");
				double simScore = 1.0;
				if(simScore > SCORE_THRESHOLD){
					final String[] s1 = PrettyGoodTokenizer.tokenize(split[0].trim());
					final String[] s2 = PrettyGoodTokenizer.tokenize(split[1].trim());
					if(docAdded<maxDocs){
						alignTraining.add(new AlignmentTrainer.TrainingPair(
								featureExtractor.extractFeatures(s1, split[0]),
								featureExtractor.extractFeatures(s2, split[1]), simScore));
						docAdded++;
					}
				}
			}
		}

		// Read data 2 and extract features 
		//String data2Path = "/Users/kartik/git/unlp-sts/sentData/aligned-good-0.67/aligned-good(0.67)";
		String data2Path = "sentData/aligned-good-0.67/aligned-good(0.67)";
		BufferedReader br = BasicFileTools.getBufferedReader(data2Path);
		String line = "";
		while((line=br.readLine()) != null){
			String[] split = line.split("\t");
			double simScore = Double.parseDouble(split[2].trim());
			if(simScore > SCORE_THRESHOLD){
				final String[] s1 = PrettyGoodTokenizer.tokenize(split[0].trim());
				final String[] s2 = PrettyGoodTokenizer.tokenize(split[1].trim());

				if(docAdded<maxDocs){
					alignTraining.add(new AlignmentTrainer.TrainingPair(
							featureExtractor.extractFeatures(s1, split[0]),
							featureExtractor.extractFeatures(s2, split[1]), simScore));
					docAdded++;
				}
			}
		}

		// Read data 3 and extract features 
		//		String data31Path = "/Users/kartik/git/unlp-sts/sentData/Tatoeba/Tatoeba.en-en.en1";
		//		String data32Path = "/Users/kartik/git/unlp-sts/sentData/Tatoeba/Tatoeba.en-en.en2";

		String data31Path = "sentData/Tatoeba/Tatoeba.en-en.en1";
		String data32Path = "sentData/Tatoeba/Tatoeba.en-en.en2";

		BufferedReader br31 = BasicFileTools.getBufferedReader(data31Path);
		BufferedReader br32 = BasicFileTools.getBufferedReader(data32Path);

		line = "";
		while((line=br31.readLine()) != null){
			String sentence31 = line;
			line = br32.readLine();
			String sentence32 = line;
			double simScore = 0.97;
			if(simScore > SCORE_THRESHOLD){
				final String[] s1 = PrettyGoodTokenizer.tokenize(sentence31.trim());
				final String[] s2 = PrettyGoodTokenizer.tokenize(sentence32.trim());
				if(docAdded<maxDocs){
					alignTraining.add(new AlignmentTrainer.TrainingPair(
							featureExtractor.extractFeatures(s1, sentence31),
							featureExtractor.extractFeatures(s2, sentence32), simScore));
					docAdded++;
				}
			}
		}

		System.out.println(alignTraining.size());

		// Extract features		
		String toSerializeModelPath = "models/bilstmComposes_" + SCORE_THRESHOLD_STRING + ".model";

		//Train alignment
		final AlignmentTrainer<BiLSTMSimAligner> trainer = new BiLSTMSimAlignmentTrainer();

		final Aligner aligner = trainer.train(alignTraining);

		aligner.save(new File(toSerializeModelPath));
		//final Aligner aligner = trainer.load(new File(toSerializeModelPath));
		//final Aligner aligner = new WordSimAligner();

		//		final List<AlignmentWithScore> alignments = new ArrayList<>();
		//
		//		// Apply aligner
		//		for(AlignmentTrainer.TrainingPair tp : alignTraining) {
		//			Alignment align = aligner.align(tp.x, tp.y);
		//			AlignmentWithScore alingWithScore = new AlignmentWithScore(align, tp.simScore);
		//			alignments.add(alingWithScore);
		//		}
		//
		//		// Train classifier
		//		//	final SimilarityClassifierTrainer<SVMRegression> clTrainer = new SVMRegressionTrainer();
		//		final SimilarityClassifier sc = new WordMoverDistance();
		//		for(AlignmentWithScore alignmentWithScore : alignments){
		//			double simScore = sc.similarity(alignmentWithScore);
		//			System.out.println(simScore);
		//		}

	}    
}
