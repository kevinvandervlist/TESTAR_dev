/***************************************************************************************************
*
* Copyright (c) 2018, 2019 Universitat Politecnica de Valencia - www.upv.es
* Copyright (c) 2018, 2019 Open Universiteit - www.ou.nl
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
* notice, this list of conditions and the following disclaimer in the
* documentation and/or other materials provided with the distribution.
* 3. Neither the name of the copyright holder nor the names of its
* contributors may be used to endorse or promote products derived from
* this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************************************/


import java.io.File;
import java.util.Set;
import nl.ou.testar.SimpleGuiStateGraph.GuiStateGraphWithVisitedActions;
import nl.ou.testar.HtmlReporting.HtmlSequenceReport;
import org.fruit.Util;
import org.fruit.alayer.Action;
import org.fruit.alayer.exceptions.*;
import org.fruit.alayer.SUT;
import org.fruit.alayer.State;
import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;
import org.fruit.alayer.Tags;
import org.sikuli.script.FindFailed;
import org.sikuli.script.Screen;
import org.testar.protocols.DesktopProtocol;

/**
 * This protocol uses:
 * - Simple state graph in selectAction() to choose new actions and select path to GUI state with new actions
 * - HTML Sequence Report
 * - SikuliX library for image recognition in executeAction() to interact with the GUI under testing
 *
 * More information on the Eye library and its API can be found from http://eyeautomate.com/eye.html
 *
 * In some cases, it is possible that TESTAR gets wrong coordinates through Windows UI Automation API, resulting
 * TESTAR to miss the controls it trying to click. Using image recognition to locate the control fixes this issue,
 * but makes TESTAR slower.
 */
public class Protocol_desktop_simple_stategraph_sikulix extends DesktopProtocol {

	private HtmlSequenceReport htmlReport;
	private int scenarioCount = 1;
	private GuiStateGraphWithVisitedActions stateGraphWithVisitedActions;

	/** 
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 * @param   settings  the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings){
		// initializing simple GUI state graph:
		stateGraphWithVisitedActions = new GuiStateGraphWithVisitedActions();
		super.initialize(settings);
	}

	/**
	 * This methods is called before each test sequence, allowing for example using external profiling software on the SUT
	 */
	@Override
	protected void preSequencePreparations() {
		//initializing the HTML sequence report:
		htmlReport = new HtmlSequenceReport(scenarioCount, sequenceCount);
		// updating scenarioCount based on existing HTML files - sequence 1 gets the correct scenarioCount:
		scenarioCount = htmlReport.getScenarioCount();
	}

	/**
	 * This method is called when the TESTAR requests the state of the SUT.
	 * Here you can add additional information to the SUT's state or write your
	 * own state fetching routine. The state should have attached an oracle
	 * (TagName: <code>Tags.OracleVerdict</code>) which describes whether the
	 * state is erroneous and if so why.
	 * @return  the current state of the SUT with attached oracle.
	 */
	@Override
	protected State getState(SUT system) throws StateBuildException{
		State state = super.getState(system);
		//adding state to the HTML sequence report:
		htmlReport.addState(state);
		return state;
	}

	/**
	 * Select one of the available actions (e.g. at random)
	 * @param state the SUT's current state
	 * @param actions the set of derived actions
	 * @return  the selected action (non-null!)
	 */
	@Override
	protected Action selectAction(State state, Set<Action> actions){
		//adding state to the HTML sequence report:
		try {
			htmlReport.addActionsAndUnvisitedActions(actions, stateGraphWithVisitedActions.getConcreteIdsOfUnvisitedActions(state));
		}catch(Exception e){
			// catching null for the first state or any new state, when unvisited actions is still null
			htmlReport.addActions(actions);
		}
		//Call the preSelectAction method from the AbstractProtocol so that, if necessary,
		//unwanted processes are killed and SUT is put into foreground.
		Action a = preSelectAction(state, actions);
		if (a!= null) {
			// returning pre-selected action
		} else{
			//if no preSelected actions are needed, then implement your own action selection strategy
			// Maintaining memory of visited states and selected actions, and selecting randomly from unvisited actions:
			a = stateGraphWithVisitedActions.selectAction(state,actions);
			//a = RandomActionSelector.selectAction(actions);
		}
		htmlReport.addSelectedAction(state.get(Tags.ScreenshotPath), a);
		return a;
	}

	/**
	 * Execute the selected action.
	 * @param system the SUT
	 * @param state the SUT's current state
	 * @param action the action to execute
	 * @return whether or not the execution succeeded
	 */
	@Override
	protected boolean executeAction(SUT system, State state, Action action){
		double waitTime = settings().get(ConfigTags.TimeToWaitAfterAction);
		try{
			double halfWait = waitTime == 0 ? 0.01 : waitTime / 2.0; // seconds
			//System.out.println("DEBUG: action: "+action.toString());
			//System.out.println("DEBUG: action short: "+action.toShortString());
			if(action.toShortString().equalsIgnoreCase("LeftClickAt")){
				String widgetScreenshotPath = protocolUtil.getActionshot(state,action);
				Screen sikuliScreen = new Screen();
				try {
					//System.out.println("DEBUG: sikuli clicking ");
					while(!new File(widgetScreenshotPath).exists()){
						//System.out.println("Waiting for image file to exist");
						Util.pause(halfWait);
					}
					Util.pause(1);
					sikuliScreen.click(widgetScreenshotPath);
				} catch (FindFailed findFailed) {
					findFailed.printStackTrace();
					return false;
				}
			}else if(action.toShortString().contains("ClickTypeInto(")){
				String textToType = action.toShortString().substring(action.toShortString().indexOf("("), action.toShortString().indexOf(")"));
				//System.out.println("parsed text:"+textToType);
				String widgetScreenshotPath = protocolUtil.getActionshot(state,action);
				Util.pause(halfWait);
				Screen sikuliScreen = new Screen();
				try {
					//System.out.println("DEBUG: sikuli typing ");
					while(!new File(widgetScreenshotPath).exists()){
						//System.out.println("Waiting for image file to exist");
						Util.pause(halfWait);
					}
					Util.pause(1);
					sikuliScreen.type(widgetScreenshotPath,textToType);
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			else {
				//System.out.println("DEBUG: TESTAR action");
				//System.out.println("DEBUG: action desc: "+action.get(Tags.Desc));
				action.run(system, state, settings().get(ConfigTags.ActionDuration));
			}return true;
		}catch(ActionFailedException afe){
			return false;
		}
	}
}
