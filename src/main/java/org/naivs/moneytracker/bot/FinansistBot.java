package org.naivs.moneytracker.bot;

import lombok.extern.slf4j.Slf4j;
import org.naivs.moneytracker.entity.Transaction;
import org.naivs.moneytracker.service.BudgetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Component
public class FinansistBot extends TelegramLongPollingBot {

	private static final String INSERT_OUT = "Расход";
	private static final String INSERT_IN = "Приход";

	private static final String ORIGINAL_AMOUNT = "Исходный бюджет";
	private static final String WASTED_AMOUNT = "Потрачено";
	private static final String AVAILABLE_BUDGET = "Остаток";

	private static final String CANCEL = "Отмена";

	private final String name;
	private final String apiKey;

	private final StateMachine stateMachine;
	private final BudgetService budgetService;

	private final ReplyKeyboardMarkup mainKeyboard;
	private final ReplyKeyboardMarkup backKeyboard;

	public FinansistBot(@Value("${bot.name}") String name, @Value("${bot.api-key}")String apiKey,
						StateMachine stateMachine, BudgetService budgetService) {
		this.name = name;
		this.apiKey = apiKey;

		this.stateMachine = stateMachine;
		this.budgetService = budgetService;

		try {
			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
			botsApi.registerBot(this);
			log.info("Finansist Bot created with name: {}; apiKey: {}", name, apiKey);
		} catch (TelegramApiException e) {
			log.error("Finansist Bot unable to create", e);
		}

		KeyboardRow row1 = new KeyboardRow();
		row1.add(KeyboardButton.builder()
				.text(INSERT_OUT)
				.build());
		row1.add(KeyboardButton.builder()
				.text(INSERT_IN)
				.build());

		KeyboardRow row2 = new KeyboardRow();
		row2.add(KeyboardButton.builder()
				.text(AVAILABLE_BUDGET)
				.build());
		row2.add(KeyboardButton.builder()
				.text(ORIGINAL_AMOUNT)
				.build());
		row2.add(KeyboardButton.builder()
				.text(WASTED_AMOUNT)
				.build());

		mainKeyboard = ReplyKeyboardMarkup.builder()
				.keyboard(List.of(row1, row2))
				.resizeKeyboard(true)
				.build();
		KeyboardRow backRow = new KeyboardRow();

		backRow.add(KeyboardButton.builder()
				.text(CANCEL)
				.build());
		backKeyboard = ReplyKeyboardMarkup.builder()
				.keyboardRow(backRow)
				.resizeKeyboard(true)
				.build();
	}

	@Override
	public String getBotUsername() {
		return name;
	}

	@Override
	public String getBotToken() {
		return apiKey;
	}

	@Override
	public void onUpdateReceived(Update update) {
		try {
			Message message = update.getMessage();
			Long chatId = message.getChatId();

			if (stateMachine.inState(chatId)) {
				BotState state = stateMachine.getState(chatId);

				if (CANCEL.equals(message.getText())) {
					stateMachine.clear(chatId);
					execute(SendMessage.builder()
							.text("Ок. Как скажешь")
							.chatId(chatId.toString())
							.allowSendingWithoutReply(true)
							.replyMarkup(mainKeyboard)
							.build());
					return;
				}

				switch (state) {
					case AWAITING_AMOUNT -> {
						try {
							BigDecimal amount = new BigDecimal(message.getText())
									.setScale(2, RoundingMode.HALF_UP);
							stateMachine.setState(chatId, BotState.AWAITING_DATE, amount);
							// ask date
							execute(SendMessage.builder()
									.chatId(chatId.toString())
									.text("А когда это было?")
									.replyMarkup(backKeyboard)
									.build());
						} catch (NumberFormatException e) {
							// error message
							execute(SendMessage.builder()
									.chatId(chatId.toString())
									.text("""
           									Чет не понял. Давай еще раз )
											Когда, говоришь?""")
									.replyMarkup(backKeyboard)
									.build());
						}
					}
					case AWAITING_DATE -> {
						try {
							int year = LocalDate.now(ZoneId.of("+3")).getYear();
							LocalDate date = LocalDate.parse(message.getText() + "." + year,
									DateTimeFormatter.ofPattern("dd.MM.yyyy"));
							stateMachine.setState(chatId, BotState.AWAITING_NOTE, date);
							// ask note
							execute(SendMessage.builder()
									.chatId(chatId.toString())
									.text("Дай-ка мне описание я запишу..")
									.replyMarkup(backKeyboard)
									.build());
						} catch (DateTimeParseException e) {
							// error message
							execute(SendMessage.builder()
									.chatId(chatId.toString())
									.text("""
											Я понимаю дату только в формате: "ДД.ММ".
											Например: 09.11.
											Так когда?""")
									.replyMarkup(backKeyboard)
									.build());
						}
					}
					case AWAITING_NOTE -> {
						String note = message.getText();
						Transaction t = stateMachine.getValue(chatId);
						t.setNote(note);
						// save transaction
						budgetService.saveTransaction(t);
						execute(SendMessage.builder()
								.chatId(chatId.toString())
								.text("Хорошо, все записал. Спс )")
								.replyMarkup(mainKeyboard)
								.build());
						stateMachine.clear(chatId);
					}
				}
				return;
			}

			if (message.hasDocument()) {
				processReport(update);
			} else {
				switch (message.getText()) {
					case INSERT_OUT -> {
						execute(SendMessage.builder()
								.chatId(chatId.toString())
								.text("Сколько потратили, мм??")
								.replyMarkup(backKeyboard)
								.build());
						// ask amount
						stateMachine.setState(chatId, BotState.AWAITING_AMOUNT, false);
					}
					case INSERT_IN -> {
						execute(SendMessage.builder()
								.chatId(chatId.toString())
								.text("О, сколько пришло?")
								.replyMarkup(backKeyboard)
								.build());
						// ask amount
						stateMachine.setState(chatId, BotState.AWAITING_AMOUNT, true);
					}
					case AVAILABLE_BUDGET -> {
						BigDecimal availableBudget = budgetService.availableBudget();
						// print budget
						execute(SendMessage.builder()
								.chatId(chatId.toString())
								.text("Остаток месячного бюджета: " + availableBudget)
								.build());
					}
					case ORIGINAL_AMOUNT -> execute(SendMessage.builder()
							.chatId(chatId.toString())
							.text("Исходный бюджет: " + budgetService.originalAmount())
							.build());
					case WASTED_AMOUNT -> execute(SendMessage.builder()
							.chatId(chatId.toString())
							.text("Потрачено: " + budgetService.wastedAmount())
							.build());
					default -> execute(SendMessage.builder()
							.text("vvv Воспользуйся меню vvv")
							.chatId(chatId.toString())
							.allowSendingWithoutReply(true)
							.replyMarkup(mainKeyboard)
							.build());
				}
			}
		} catch (TelegramApiException e) {
			log.error("message not sent", e);
		}
	}

	private void processReport(Update update) {
		try {
			GetFile getFile = new GetFile(update.getMessage().getDocument().getFileId());
			File file = execute(getFile);
			java.io.File file1 = downloadFile(file.getFilePath());

			budgetService.saveFinanceReport(file1);
		} catch (TelegramApiException e) {
			log.error("Failed to download file", e);
		}
	}
}
