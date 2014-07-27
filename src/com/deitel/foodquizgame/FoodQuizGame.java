package com.deitel.foodquizgame;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TableRow;

public class FoodQuizGame extends Activity {

	// String used when logging error messages
	private static final String TAG = "FoodQuizGame Activity";

	private List<String> fileNameList; // food file names
	private List<String> quizDishesList; // names of dishes in quiz
	private Map<String, Boolean> categoriesMap; // which categories are enabled
	private String correctAnswer; // correct dish name for the current dish
									// shown
	private int totalGuesses; // number of guesses made
	private int correctAnswers; // number of correct guesses
	private int guessRows; // number of rows displaying choices
	private Random random;
	private Handler handler; // used to delay loading next dish
	private Animation shakeAnimation; // animation for incorrect guess

	private TextView answerTextView;
	private TextView questionNumberTextView;
	private ImageView foodImageView;
	private TableLayout buttonTableLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // call the superclass's methods
		setContentView(R.layout.main);

		fileNameList = new ArrayList<String>(); // list of image file names
		quizDishesList = new ArrayList<String>(); // dishes in the quiz
		categoriesMap = new HashMap<String, Boolean>(); // HashMap of categories
		guessRows = 1;
		random = new Random();
		handler = new Handler();

		shakeAnimation = AnimationUtils.loadAnimation(this,
				R.anim.incorrect_shake);
		shakeAnimation.setRepeatCount(3);

		String[] categoryList = getResources().getStringArray(
				R.array.categoryList);

		for (String category : categoryList) {
			categoriesMap.put(category, true); // enable all categories by
												// default
		}
		questionNumberTextView = (TextView) findViewById(R.id.questionNumberTextView);
		foodImageView = (ImageView) findViewById(R.id.foodImageView);
		buttonTableLayout = (TableLayout) findViewById(R.id.buttonTableLayout);
		answerTextView = (TextView) findViewById(R.id.answerTextView);

		questionNumberTextView.setText(getResources().getString(
				R.string.question)
				+ " 1 " + getResources().getString(R.string.of) + " 10");

		resetQuiz(); // starts a new quiz

	} // end method onCreate

	private void resetQuiz() {

		AssetManager assets = getAssets();
		fileNameList.clear(); // empty the list of image file names
		try {
			Set<String> categories = categoriesMap.keySet();
			// loop through each categories (holds British, Chinese etc)
			for (String category : categories) {

				if (categoriesMap.get(category)) // if category is enabled
				{
					// get a list of dish image files in this category
					String[] paths = assets.list(category);

					for (String path : paths)
						fileNameList.add(path.replace(".jpg", ""));

				}// end if
			}// end for
		}// end try
		catch (IOException e) {
			Log.e(TAG, "Error loading image file names", e);
		} // end catch
		correctAnswers = 0;
		totalGuesses = 0;
		quizDishesList.clear(); // clear prior list of quiz dishes

		int dishCounter = 1;
		int numberOfDishes = fileNameList.size(); // get number of dishes

		while (dishCounter <= 10) {
			int randomIndex = random.nextInt(numberOfDishes); // generate the
																// index in the
																// range of 0 to
																// one less than
																// the number of
																// dishes/ i.e 0
																// - 39

			// get the random file name
			String fileName = fileNameList.get(randomIndex);

			// if the region is enabled and it hasn't already been chosen
			if (!quizDishesList.contains(fileName)) {
				quizDishesList.add(fileName); // contains 10 dishes for the
												// current quiz
				++dishCounter;
			}// end if
		}// end while
		loadNextDish(); // start the quiz by loading the first dish
	}

	private void loadNextDish() {

		// get file name of the next dish and remove it from the list
		String nextImageName = quizDishesList.remove(0);
		correctAnswer = nextImageName;

		answerTextView.setText(""); // clear answerTextView

		// display the number of current question in the quiz
		questionNumberTextView.setText(getResources().getString(
				R.string.question)
				+ " "
				+ (correctAnswers + 1)
				+ " "
				+ getResources().getString(R.string.of) + " 10");
		// extract the category from the next image's name
		String category = nextImageName
				.substring(0, nextImageName.indexOf("-"));

		// use AssetManager to load next image from assets folder
		AssetManager assets = getAssets();
		InputStream stream;

		try {
			stream = assets.open(category + "/" + nextImageName + ".jpg");

			// load the asset as a Drawable and display on the flagImageView
			Drawable dish = Drawable.createFromStream(stream, nextImageName);
			foodImageView.setImageDrawable(dish);
		} catch (IOException e) {
			Log.e(TAG, "Error loading " + nextImageName, e);

		}
		for (int row = 0; row < buttonTableLayout.getChildCount(); ++row)
			((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();

		Collections.shuffle(fileNameList); // shuffle file names
		// put the correct answer at the end of fileNameList
		int correct = fileNameList.indexOf(correctAnswer);
		fileNameList.add(fileNameList.remove(correct)); // correct answer at the
														// end of fileNameList

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// add 3, 6, or 9 answer Buttons based on the value of guessRows
		for (int row = 0; row < guessRows; row++) {
			TableRow currentTableRow = getTableRow(row);

			for (int column = 0; column < 3; column++) {
				// inflate guess_button.xml to create new buttons
				Button newGuessButton = (Button) inflater.inflate(
						R.layout.guess_button, null);

				// get dish name and set as newGuessButton's text
				String filename = fileNameList.get((row * 3) + column);
				newGuessButton.setText(getDishName(filename));

				// register answerButtonListener to respond to button clicks
				newGuessButton.setOnClickListener(guessButtonListener);
				currentTableRow.addView(newGuessButton);
			} // end for
		}// end outer for

		int row = random.nextInt(guessRows); // pick a random row
		int column = random.nextInt(3);
		TableRow randomTableRow = getTableRow(row);
		String dishName = getDishName(correctAnswer);
		((Button) randomTableRow.getChildAt(column)).setText(dishName);

	} // end loadNextFlag

	private String getDishName(String name) {

		return name.substring(name.indexOf('-') + 1).replace('_', ' ');
	}

	private TableRow getTableRow(int row) {

		return (TableRow) buttonTableLayout.getChildAt(row);
	}

	private OnClickListener guessButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			submitGuess((Button) v);
		}
	};

	private void submitGuess(Button guessButton) {

		String guess = guessButton.getText().toString();
		String answer = getDishName(correctAnswer);
		++totalGuesses; // increment the number of guesses the has made

		if (guess.equals(answer)) {
			++correctAnswers;
			answerTextView.setText(answer + "!");
			answerTextView.setTextColor(getResources().getColor(
					R.color.correct_answer));

			disableButtons();

			// if the user has correctly identified 10 dishes
			if (correctAnswers == 10) {
				// create new AlertDialog builder
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.reset_quiz);
				builder.setMessage(String.format("%d %s, %.02f%% %s",
						totalGuesses, getResources()
								.getString(R.string.guesses),
						(1000 / (double) totalGuesses), getResources()
								.getString(R.string.correct)));
				builder.setCancelable(false);
				// add "Reset Quiz" Button
				builder.setPositiveButton(R.string.reset_quiz,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								resetQuiz();

							}
						}); // end anonymous inner class
				AlertDialog resetDialog = builder.create();
				resetDialog.show();

			} // end if
			else {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						loadNextDish();
					}
				}, 1000);

			}// end else
		}// end outer if
		else { // guess was incorrect
				// play animation
			foodImageView.startAnimation(shakeAnimation);
			answerTextView.setText(R.string.incorrect_answer);
			answerTextView.setTextColor(getResources().getColor(
					R.color.incorrect_answer));
			guessButton.setEnabled(false);
		} // end else
	}// end method submitGuess

	private void disableButtons() {
		for (int row = 0; row < buttonTableLayout.getChildCount(); ++row) {
			TableRow tableRow = (TableRow) buttonTableLayout.getChildAt(row);
			for (int i = 0; i < tableRow.getChildCount(); ++i) {
				tableRow.getChildAt(i).setEnabled(false);
			}
		}

	}

	private final int CHOICES_MENU_ID = Menu.FIRST;
	private final int CATEGORIES_MENU_ID = Menu.FIRST + 1;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(Menu.NONE, CHOICES_MENU_ID, Menu.NONE, R.string.choices);
		menu.add(Menu.NONE, CATEGORIES_MENU_ID, Menu.NONE, R.string.categories);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// switch the menu id of the user-selected option
		switch (item.getItemId()) {
		case CHOICES_MENU_ID:
			// create a list of the possible numbers of answer choices
			final String[] possibleChoices = getResources().getStringArray(
					R.array.guessesList);
			AlertDialog.Builder choicesBuilder = new AlertDialog.Builder(this);
			choicesBuilder.setTitle(R.string.choices);

			// add possibleChoices items to the Dialog and set the
			// behaviour when one of the items is clicked
			choicesBuilder.setItems(R.array.guessesList,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							// update guessRows to match the user's choice
							guessRows = Integer.parseInt(possibleChoices[item]
									.toString()) / 3;
							resetQuiz();
						}// end method onClick
					}// end anonymous inner class
					);
			AlertDialog choicesDialog = choicesBuilder.create();
			choicesDialog.show();
			return true;
			
		case CATEGORIES_MENU_ID:
			//get array of world regions
			final String [] categoryNames = categoriesMap.keySet().toArray(new String[categoriesMap.size()]);
			
			//boolean array representing whether each category is enabled
			boolean [] categoryEnabled = new boolean [categoriesMap.size()];
			for (int i = 0; i <categoryEnabled.length; ++i) {
				categoryEnabled[i] = categoriesMap.get(categoryNames[i]);
			} 
			
			AlertDialog.Builder categoryBuilder = new AlertDialog.Builder(this);
			categoryBuilder.setTitle(R.string.categories);
			
			//replace _ with space in category name for displaying purpose
			String [] displayNames = new String [categoryNames.length];
			for (int i = 0; i < categoryNames.length; ++i) 
				displayNames [i] = categoryNames[i].replace('_', ' ');
			
				//add displayNames to the Dialog and set the behaviour 
				//when one of the items is clicked
				
				categoryBuilder.setMultiChoiceItems(displayNames, categoryEnabled, new DialogInterface.OnMultiChoiceClickListener() 
				{
					
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						//include or exclude the clicked category
						//depending on whether or not it's checked
						categoriesMap.put(categoryNames[which].toString(), isChecked);
						
						
					}//end method onClick
				}//end anonymous inner class
				); //end call to setMultipleChoiceItems
			
			//resets quiz when user presses the "Reset Quiz" Button
			categoryBuilder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick (DialogInterface dialog, int button) {
					resetQuiz(); 
				}//end method onClick
			}// end anonymous inner class
		); // end call to method setPositiveButton
			
			AlertDialog categoryDialog = categoryBuilder.create();
			categoryDialog.show(); //display the dialog
			return true;
		}// end switch
		
		return super.onOptionsItemSelected(item);
	}// end method onOptionsItemSelected
}// end class
