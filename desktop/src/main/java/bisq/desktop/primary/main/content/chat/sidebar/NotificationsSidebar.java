/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.primary.main.content.chat.sidebar;

import bisq.chat.ChatService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.i18n.Res;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NotificationsSidebar {

    private final Controller controller;

    public NotificationsSidebar(ChatService chatService) {
        controller = new Controller(chatService);
    }

    public void setChannel(ChatChannel<? extends ChatMessage> chatChannel) {
        controller.applyChannel(chatChannel);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;
        private Pin tradeChannelSelectionPin, discussionChannelSelectionPin, eventsChannelSelectionPin, supportChannelSelectionPin;
        private Pin notificationTypePin;

        private Controller(ChatService chatService) {
            this.chatService = chatService;
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            tradeChannelSelectionPin = chatService.getBisqEasyChatChannelSelectionService().getSelectedChannel().addObserver(this::onChannelChanged);
            discussionChannelSelectionPin = chatService.getDiscussionChatChannelSelectionService().getSelectedChannel().addObserver(this::onChannelChanged);
            eventsChannelSelectionPin = chatService.getEventsChatChannelSelectionService().getSelectedChannel().addObserver(this::onChannelChanged);
            supportChannelSelectionPin = chatService.getSupportChatChannelSelectionService().getSelectedChannel().addObserver(this::onChannelChanged);
        }


        @Override
        public void onDeactivate() {
            tradeChannelSelectionPin.unbind();
            discussionChannelSelectionPin.unbind();
            eventsChannelSelectionPin.unbind();
            supportChannelSelectionPin.unbind();
            if (notificationTypePin != null) {
                notificationTypePin.unbind();
            }
        }

        private void applyChannel(ChatChannel<? extends ChatMessage> chatChannel) {
            model.channel.set(chatChannel);
            model.notificationType.set(chatChannel.getChatChannelNotificationType().get());
        }

        private void onChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
            if (notificationTypePin != null) {
                notificationTypePin.unbind();
            }
            if (chatChannel != null) {
                notificationTypePin = FxBindings.bind(model.notificationType).to(chatChannel.getChatChannelNotificationType());
            }
        }

        void onSetNotificationType(ChatChannelNotificationType type) {
            model.notificationType.set(type);
            ChatChannel<? extends ChatMessage> chatChannel = model.channel.get();
            if (chatChannel != null) {
                chatService.getChatChannelService(chatChannel).setChatChannelNotificationType(chatChannel, type);
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ChatChannel<? extends ChatMessage>> channel = new SimpleObjectProperty<>();
        private final ObjectProperty<ChatChannelNotificationType> notificationType = new SimpleObjectProperty<>(ChatChannelNotificationType.GLOBAL_DEFAULT);

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ToggleGroup toggleGroup = new ToggleGroup();
        private final ChangeListener<Toggle> toggleListener;
        private final RadioButton globalDefault, all, mention, off;

        private final ChangeListener<ChatChannel<? extends ChatMessage>> channelChangeListener;
        private Subscription selectedNotificationTypePin;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);

            Label headline = new Label(Res.get("social.channel.notifications"));
            headline.setId("chat-sidebar-title");

            globalDefault = new RadioButton(Res.get("social.channel.notifications.globalDefault"));
            all = new RadioButton(Res.get("social.channel.notifications.all"));
            mention = new RadioButton(Res.get("social.channel.notifications.mention"));
            off = new RadioButton(Res.get("social.channel.notifications.off"));

            globalDefault.setToggleGroup(toggleGroup);
            all.setToggleGroup(toggleGroup);
            mention.setToggleGroup(toggleGroup);
            off.setToggleGroup(toggleGroup);

            globalDefault.setUserData(ChatChannelNotificationType.GLOBAL_DEFAULT);
            all.setUserData(ChatChannelNotificationType.ALL);
            mention.setUserData(ChatChannelNotificationType.MENTION);
            off.setUserData(ChatChannelNotificationType.OFF);

            VBox vBox = new VBox(10, globalDefault, all, mention, off);
            vBox.setPadding(new Insets(10));
            vBox.getStyleClass().add("bisq-dark-bg");

            root.getChildren().addAll(headline, vBox);

            toggleListener = (observable, oldValue, newValue) -> controller.onSetNotificationType((ChatChannelNotificationType) newValue.getUserData());
            channelChangeListener = (observable, oldValue, newValue) -> applySelectedNotificationType();
        }

        private void applySelectedNotificationType() {
            switch (model.notificationType.get()) {
                case GLOBAL_DEFAULT:
                    toggleGroup.selectToggle(globalDefault);
                    break;
                case ALL: {
                    toggleGroup.selectToggle(all);
                    break;
                }
                case MENTION: {
                    toggleGroup.selectToggle(mention);
                    break;
                }
                case OFF: {
                    toggleGroup.selectToggle(off);
                    break;
                }
            }
        }

        @Override
        protected void onViewAttached() {
            model.channel.addListener(channelChangeListener);
            toggleGroup.selectedToggleProperty().addListener(toggleListener);
            selectedNotificationTypePin = EasyBind.subscribe(model.notificationType, selected -> applySelectedNotificationType());
        }

        @Override
        protected void onViewDetached() {
            model.channel.removeListener(channelChangeListener);
            toggleGroup.selectedToggleProperty().removeListener(toggleListener);
            selectedNotificationTypePin.unsubscribe();
        }
    }
}