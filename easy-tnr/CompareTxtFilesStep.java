package calypso.lbp.qa.easy.tnr.step.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lbp.qa.easy.tnr.bean.Step;
import lbp.qa.easy.tnr.execution.exception.StepExecutionException;
import lbp.qa.easy.tnr.execution.log.Log;
import lbp.qa.easy.tnr.util.StepAttributeConstants;

import org.apache.commons.lang3.StringUtils;

import calypso.lbp.qa.easy.tnr.util.EaiStepAttributeConstantes;
public class CompareTxtFilesStep extends Step {

	private static final long serialVersionUID = 4655233964123650861L;

	@Override
	public boolean doSteplet(List<String> messages, Map<String, Object> collector) throws StepExecutionException {
		String prodFileName = getDynamicAttributeValue(StepAttributeConstants.ATTR_PROD_SOURCE);
		String testFileName = getDynamicAttributeValue(StepAttributeConstants.ATTR_TEST_SOURCE);
		String ignoreLineOrder = getDynamicAttributeValue(EaiStepAttributeConstantes.ATTR_CMP_TXT_FILE_IGNORE_LINES_ORDER);
		if(ignoreLineOrder.equals("No")){
			try {
				String prodFileContent = readFile(prodFileName);
				String testFileContent = readFile(testFileName);
				String result = StringUtils.difference(prodFileContent, testFileContent);
				String result2 = StringUtils.difference(testFileContent,prodFileContent);
				if (StringUtils.EMPTY.equals(result) && StringUtils.EMPTY.equals(result2)) {
					messages.add("No differences found.");
					return true;
				} else {
					Log.error("Difference found in test file: " + result);
					messages.add("Difference found in test file: " + result);
					return false;
				}
			} catch (IOException e) {
				Log.error(this, e);
				return false;
			}
		}
		else{
			try {
				List<String> prodFileContentAsList = readFileInList(prodFileName);
				List<String> testFileContentAsList = readFileInList(testFileName);
				if(areListsEqualUsingSet(prodFileContentAsList,testFileContentAsList)){
					messages.add("No differences found.");
					return true;
				}
				else {
					Log.error("Difference found in test file" );
					messages.add("Difference found in test file");
					return false;
				}
			} catch (IOException e) {
				Log.error(this, e);
				return false;
			}
			
		}
		}

	@Override
	public void setAttributes() {
		addAttribute(StepAttributeConstants.ATTR_PROD_SOURCE, "Production file path", null, true);
		addAttribute(StepAttributeConstants.ATTR_TEST_SOURCE, "Current file path", null, true);
		addAttribute(EaiStepAttributeConstantes.ATTR_CMP_TXT_FILE_CASE_SENSITIVE, "Case sensitive ?",
				StepAttributeConstants.YES_NO, true);
		addAttribute(EaiStepAttributeConstantes.ATTR_CMP_TXT_FILE_IGNORE_LINES_ORDER, "Ignore lines order ?",
				StepAttributeConstants.YES_NO, false);
	}

	@Override
	public String getUsage() {
		return "Compare two txt files";
	}

	private String readFile(String fileName) throws IOException {
		StringBuffer bfr = new StringBuffer();
		BufferedReader br = null;

		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(fileName));
			while ((sCurrentLine = br.readLine()) != null) {
				bfr.append(sCurrentLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return bfr.toString();
	}
	private List<String> readFileInList(String fileName) throws IOException {
		BufferedReader br = null;
		List<String> result = new ArrayList<String>();
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(fileName));
			while ((sCurrentLine = br.readLine()) != null) {
				result.add(sCurrentLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}
	  private boolean areListsEqualUsingSet(List<String> list1, List<String> list2) {
	        return list1.size()==list2.size() && new HashSet<>(list1).equals(new HashSet<>(list2));
	    }
	  private void getDiffBetweenSet(Set set1, Set set2,List<String> messages){
		  set1.removeAll(set2);
	  }
}
